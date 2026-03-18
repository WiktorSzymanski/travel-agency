package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueEntry
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort

abstract class PersistentSaga(
    private val sagaRepository: SagaRepository,
    private val outboxPort: OutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
    private val sagaState: SagaState
) {
    private val delayManager: DelayManager = DelayManager(sagaState.retryCount)
    private val maxRetries = 5

    private val metadata: Metadata
        get() = Metadata(
            correlationId = sagaState.id,
            revision = sagaState.version,
        )

    suspend fun executeOrResume() {
        when (sagaState.status) {
            SagaStatus.COMPLETED -> return
            SagaStatus.FAILED -> return
            SagaStatus.PROCESSING -> resume()
            SagaStatus.NEW -> startSaga()
        }
    }

    suspend fun resume() {
        when (sagaState.step) {
            SagaStep.PENDING_COMMUTE -> handleCommuteStep()
            SagaStep.PENDING_ACCOMMODATION -> handleAccommodationStep()
            SagaStep.PENDING_ATTRACTION -> handleAttractionStep()
            SagaStep.COMPENSATING_ACCOMMODATION -> compensateAccommodation()
            SagaStep.COMPENSATING_COMMUTE -> compensateCommute()
            SagaStep.IDLE -> return
        }
    }

    private suspend fun startSaga() {
//        sagaRepository.save(sagaState) // TODO: redundant???

        sagaState.status = SagaStatus.PROCESSING
        sagaState.step = SagaStep.PENDING_COMMUTE
        outboxPort.save(
            sagaState.copy(),
            listOf(getSagaStartedEvent()),
            metadata,
            sagaRepository
        )

        handleCommuteStep()
    }

    private suspend fun handleCommuteStep() {
        while(true) {
            try {
                commuteStep()
                return handleAccommodationStep()
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    sagaState.message = e.message
                    persistState()
                    return failSaga()
                }
                sagaState.retryCount++
                persistState()
                delay(delayManager.getCurrentDelay())
            }
        }
    }

    private suspend fun handleAccommodationStep() {
        sagaState.step = SagaStep.PENDING_ACCOMMODATION
        sagaState.retryCount = 0
        persistState()
        while(true) {
            try {
                accommodationStep()
                return handleAttractionStep()
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    sagaState.message = e.message
                    persistState()
                    return compensateCommute()
                }

                sagaState.retryCount++
                persistState()
                delay(delayManager.getCurrentDelay())
            }
        }
    }

    private suspend fun handleAttractionStep() {
        sagaState.step = SagaStep.PENDING_ATTRACTION
        sagaState.retryCount = 0
        persistState()

        while (true) {
            try {
                attractionStep()
                break
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    sagaState.message = e.message
                    persistState()
                    return compensateAccommodation()
                }

                sagaState.retryCount++
                persistState()
                delay(delayManager.getCurrentDelay())
            }
        }

        completeSaga()
    }

    private suspend fun completeSaga() {
        sagaState.status = SagaStatus.COMPLETED
        sagaState.step = SagaStep.IDLE
        sagaState.version++

        outboxPort.save(
            sagaState.copy(),
            listOf(getSagaCompletedEvent()),
            metadata,
            sagaRepository
        )
    }

    private suspend fun failSaga() {
        sagaState.status = SagaStatus.FAILED
        sagaState.step = SagaStep.IDLE
        sagaState.version++

        outboxPort.save(
            sagaState.copy(),
            listOf(getSagaFailedEvent()),
            metadata,
            sagaRepository
        )
    }

    private suspend fun compensateCommute() {
        sagaState.step = SagaStep.COMPENSATING_COMMUTE
        sagaState.retryCount = 0
        persistState()
        while(true) {
            try {
                compensateCommuteStep()
                break
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    deadLetterQueueRepository.save(
                        DeadLetterQueueEntry(
                            message = "sagaId: ${sagaState.id}, aggregateId = ${sagaState.travelOffer.commuteId}, message = ${e.message ?: "Unknown error"}"
                        )
                    )
                    break
                }

                sagaState.retryCount++
                persistState()
                delay(delayManager.getCurrentDelay())
            }
        }

        failSaga()
    }

    private suspend fun compensateAccommodation() {
        sagaState.step = SagaStep.COMPENSATING_ACCOMMODATION
        sagaState.retryCount = 0
        persistState()
        while(true) {
            try {
                compensateAccommodationStep()
                break
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    deadLetterQueueRepository.save(
                        DeadLetterQueueEntry(
                            message = "sagaId: ${sagaState.id}, aggregateId = ${sagaState.travelOffer.accommodationId}, message = ${e.message ?: "Unknown error"}"
                        )
                    )
                    break
                }
                sagaState.retryCount++
                persistState()
                delay(delayManager.getCurrentDelay())
            }
        }
        compensateCommute()
    }

    private suspend fun persistState() {
        sagaState.version++
        sagaRepository.save(sagaState)
    }

    abstract suspend fun accommodationStep()

    abstract suspend fun compensateAccommodationStep()

    abstract suspend fun commuteStep()

    abstract suspend fun compensateCommuteStep()

    abstract suspend fun attractionStep()

    abstract fun getSagaStartedEvent(): SagaEvent

    abstract fun getSagaCompletedEvent(): SagaEvent

    abstract fun getSagaFailedEvent(): SagaEvent
}
