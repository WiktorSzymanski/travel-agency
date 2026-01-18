package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.*
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.event.SagaEvent

abstract class PersistentSaga(
    private val commandBus: CommandBus,
    private val sagaRepository: SagaRepository,
    private val sagaOutboxPort: SagaOutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
    private val sagaState: SagaState
) {
    companion object {
        fun getSagaInstance(
            commandBus: CommandBus,
            sagaRepository: SagaRepository,
            sagaOutboxPort: SagaOutboxPort,
            deadLetterQueueRepository: DeadLetterQueueRepository,
            sagaState: SagaState,
            metadata: Metadata
        ) {
            when (sagaState.type) {
                SagaType.BOOKING -> BookingSaga(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository, sagaState, metadata)
                SagaType.CANCELLING -> CancelBookingSaga(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository, sagaState, metadata)
            }
        }
    }

    private val delayManager: DelayManager = DelayManager(sagaState.retryCount)
    private val maxRetries = 5

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
        sagaRepository.save(sagaState)

        sagaState.status = SagaStatus.PROCESSING
        sagaState.step = SagaStep.PENDING_COMMUTE
        sagaOutboxPort.saveStateWithEvent(
            sagaState.copy(),
            getSagaStartedEvent()
        )

        handleCommuteStep()
    }

    private suspend fun handleCommuteStep() {
        while(true) {
            try {
                commandBus.dispatch<CommuteCommand, Commute>(getCommuteCommand())
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
                commandBus.dispatch<AccommodationCommand, Accommodation>(getAccommodationCommand())
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
        getAttractionCommand()?.let {
            while (true) {
                try {
                    commandBus.dispatch<AttractionCommand, Attraction>(it)
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
        }
        completeSaga()
    }

    private suspend fun completeSaga() {
        sagaState.status = SagaStatus.COMPLETED
        sagaState.step = SagaStep.IDLE

        sagaOutboxPort.saveStateWithEvent(
            sagaState.copy(),
            getSagaCompletedEvent()
        )
    }

    private suspend fun failSaga() {
        sagaState.status = SagaStatus.FAILED
        sagaState.step = SagaStep.IDLE

        sagaOutboxPort.saveStateWithEvent(
            sagaState.copy(),
            getSagaFailedEvent()
        )
    }

    private suspend fun compensateCommute() {
        sagaState.step = SagaStep.COMPENSATING_COMMUTE
        sagaState.retryCount = 0
        persistState()
        while(true) {
            try {
                commandBus.dispatch<CommuteCommand, Commute>(getCompensateCommuteCommand())
                break
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    deadLetterQueueRepository.save(
                        DeadLetterQueueEntry(
                            sagaId = sagaState.id,
                            aggregateId = sagaState.travelOffer.commuteId,
                            message = e.message ?: "Unknown error"
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
                commandBus.dispatch<AccommodationCommand, Accommodation>(getCompensateAccommodationCommand())
                break
            } catch (e: Exception) {
                if (!ConcurrentModificationException::class.isInstance(e) || sagaState.retryCount >= maxRetries) {
                    deadLetterQueueRepository.save(
                        DeadLetterQueueEntry(
                            sagaId = sagaState.id,
                            aggregateId = sagaState.travelOffer.accommodationId,
                            message = e.message ?: "Unknown error"
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

    abstract fun getAccommodationCommand(): AccommodationCommand

    abstract fun getCompensateAccommodationCommand(): CompensateAccommodationCommand

    abstract fun getCommuteCommand(): CommuteCommand

    abstract fun getCompensateCommuteCommand(): CompensateCommuteCommand

    abstract fun getAttractionCommand(): AttractionCommand?

    abstract fun getSagaStartedEvent(): EventEnvelope<SagaEvent>

    abstract fun getSagaCompletedEvent(): EventEnvelope<SagaEvent>

    abstract fun getSagaFailedEvent(): EventEnvelope<SagaEvent>
}
