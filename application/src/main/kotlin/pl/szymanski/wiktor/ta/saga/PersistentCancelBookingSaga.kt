package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent

class PersistentCancelBookingSaga (
    private val commandBus: CommandBus,
    private val sagaRepository: SagaRepository,
    private val sagaOutboxPort: SagaOutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
    private val sagaState: SagaState,
    private val metadata: Metadata
) {
    private val delayManager: DelayManager = DelayManager(sagaState.retryCount)
    private val maxRetries = 5


    private val accommodationCommand: AccommodationCommand =
        CancelAccommodationBookingCommand(
            sagaState.travelOffer.accommodationId,
            metadata.correlationId,
            sagaState.bookingId
        )

    private fun getCompensateAccommodationCommand(): CompensateAccommodationCommand {
        return CompensateCancelAccommodationBookingCommand(
            accommodationId = sagaState.travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId
        )
    }

    private val commuteCommand: CommuteCommand =
        CancelCommuteBookingCommand(
            sagaState.travelOffer.commuteId,
            metadata.correlationId,
            sagaState.bookingId,
        )

    private fun getCompensateCommuteCommand(): CompensateCommuteCommand {
        return CompensateCancelCommuteBookingCommand(
            commuteId = sagaState.travelOffer.commuteId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId,
            seat = sagaState.seat,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> CancelAttractionBookingCommand(
                attractionId,
                metadata.correlationId,
                sagaState.bookingId
            )
            is AttractionId.Empty -> null
        }

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
            EventEnvelope(BookingCancelSagaStartedEvent(
                bookingId = sagaState.bookingId
            ), metadata)
        )

        handleCommuteStep()
    }

    private suspend fun handleCommuteStep() {
        while(true) {
            try {
                commandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
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
                commandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand)
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
        attractionCommand?.let {
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
            EventEnvelope(BookingCancelSagaCompletedEvent(
                bookingId = sagaState.bookingId,
                seat = sagaState.seat),
                metadata)
        )
    }

    private suspend fun failSaga() {
        sagaState.status = SagaStatus.FAILED
        sagaState.step = SagaStep.IDLE

        sagaOutboxPort.saveStateWithEvent(
            sagaState.copy(),
            EventEnvelope(BookingCancelSagaFailedEvent(
                bookingId = sagaState.bookingId,
                message = sagaState.message!!),
                metadata)
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
}