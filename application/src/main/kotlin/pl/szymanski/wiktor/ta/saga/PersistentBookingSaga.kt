package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.exception.CommuteException
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class PersistentBookingSaga(
    private val id: UUID,
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val stateRepo: SagaRepository,
    private val travelOffer: TravelOffer,
    private val seat: Seat,
    private val bookingId: BookingId,
    private val metadata: Metadata
) {
    private val accommodationCommand: AccommodationCommand =
        BookAccommodationCommand(
            travelOffer.accommodationId,
            metadata.correlationId,
            bookingId,
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateBookAccommodationCommand(
            accommodationId = travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            eventId = eventId,
            bookingId = bookingId,
        )
    }

    private var commuteCommand: CommuteCommand =
        BookCommuteCommand(
            travelOffer.commuteId,
            metadata.correlationId,
            bookingId,
            seat,
        )

    private fun getCompensateCommuteCommand(eventId: UUID): CompensateCommuteCommand {
        return CompensateBookCommuteCommand(
            commuteId = travelOffer.commuteId,
            correlationId = metadata.correlationId,
            eventId = eventId,
            bookingId = bookingId,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = travelOffer.attractionId) {
            is AttractionId.Present -> BookAttractionCommand(
                attractionId,
                metadata.correlationId,
                bookingId,
            )
            is AttractionId.Empty -> null
        }

    private val maxRetries = 5

    suspend fun executeOrResume() {
        val state = stateRepo.findById(id) ?: createInitialState()
        when (state.status) {
            SagaStatus.COMPLETED -> return
            SagaStatus.FAILED -> compensateAndFail(state, "Saga failed")
            SagaStatus.COMMUTE_PENDING -> handleCommuteStep(state)
            SagaStatus.ACCOMMODATION_PENDING -> handleAccommodationStep(state)
            SagaStatus.ATTRACTION_PENDING -> handleAttractionStep(state)
            SagaStatus.NEW -> startSaga(state)
        }

    }

    private suspend fun createInitialState(): SagaState {
        val state = SagaState(
            type = SagaType.BOOKING,
            bookingId = bookingId,
            travelOffer = travelOffer,
            seat = seat
        )
        stateRepo.save(state)

        startSaga(state)
        return state
    }

    private suspend fun startSaga(state: SagaState) {
        eventBus.publish(EventEnvelope(BookingSagaStartedEvent(
            bookingId = bookingId
        ), metadata))
        handleCommuteStep(state)
    }

    private suspend fun handleCommuteStep(state: SagaState) {
        persistStatus(state, SagaStatus.COMMUTE_PENDING)
        try {
            val (_, events) = withRetry(maxRetries) {
                commandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
            }
            state.sagaContext.commuteEventId = events.first().eventId
            persistState(state)
            handleAccommodationStep(state)
        } catch (e: CommuteException) {
            // TODO: to trochę nie ma sensu bo withRetry robi maxRetries po czym throw-uje ostatni.
            if (state.retryCount < maxRetries) {
                state.incrementRetryCount()
                stateRepo.save(state)
            } else {
                compensateAndFail(state, e.message!!)
            }
        }
    }

    private suspend fun handleAccommodationStep(state: SagaState) {
        persistStatus(state, SagaStatus.ACCOMMODATION_PENDING)
        try {
            val (_, events) = withRetry(maxRetries - state.retryCount) {
                commandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand)
            }
            state.sagaContext.accommodationEventId = events.first().eventId
            persistState(state)
            if (attractionCommand != null) handleAttractionStep(state)
            else completeSaga(state)
        } catch (e: Exception) {
            // TODO: to trochę nie ma sensu bo withRetry robi maxRetries po czym throw-uje ostatni.
            if (state.retryCount < maxRetries) {
                state.incrementRetryCount()
                stateRepo.save(state)
            } else {
                compensateAndFail(state, e.message!!)
            }
        }
    }

    private suspend fun handleAttractionStep(state: SagaState) {
        persistStatus(state, SagaStatus.ATTRACTION_PENDING)
        try {
            withRetry(maxRetries) { commandBus.dispatch<AttractionCommand, Attraction>(attractionCommand!!) }
            completeSaga(state)
        } catch (e: Exception) {
            if (state.retryCount < maxRetries) {
                state.incrementRetryCount()
                stateRepo.save(state)
            } else {
                compensateAndFail(state, e.message!!)
            }
        }
    }

    private suspend fun completeSaga(state: SagaState) {
        state.status = SagaStatus.COMPLETED
        stateRepo.save(state)
        eventBus.publish(EventEnvelope(BookingSagaCompletedEvent(
            bookingId = bookingId,
            seat = seat), metadata))
    }

    private suspend fun compensateAndFail(state: SagaState, message: String) {
        compensateAccommodation(state)
        compensateCommute(state)
        state.status = SagaStatus.FAILED
        stateRepo.save(state)
        eventBus.publish(EventEnvelope(BookingSagaFailedEvent(
            bookingId = bookingId,
            message = message), metadata))
    }

    private suspend fun compensateCommute(state: SagaState) {
        // TODO: add withRetry and ensure compensations are persisted if system were to fail
        state.sagaContext.commuteEventId?.let {
            withRetry(maxRetries) { commandBus.dispatch<CommuteCommand, Commute>(getCompensateCommuteCommand(it)) }
        }
    }

    private suspend fun compensateAccommodation(state: SagaState) {
        // TODO: add withRetry and ensure compensations are persisted if system were to fail
        state.sagaContext.accommodationEventId?.let {
            commandBus.dispatchAndForget(getCompensateAccommodationCommand(it))
        }
    }

    private suspend fun persistStatus(state: SagaState, status: SagaStatus) {
        state.status = status
        state.incrementVersion()
        stateRepo.save(state)
    }

    private suspend fun persistState(state: SagaState) {
        state.incrementVersion()
        stateRepo.save(state)
    }
}
