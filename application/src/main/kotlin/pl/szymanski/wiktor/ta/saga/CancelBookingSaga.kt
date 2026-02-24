package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort

class CancelBookingSaga(
    sagaRepository: SagaRepository,
    outboxPort: OutboxPort,
    deadLetterQueueRepository: DeadLetterQueueRepository,
    private val cancelAccommodationBookingCommandHandler: CancelAccommodationBookingCommandHandler,
    private val cancelCommuteBookingCommandHandler: CancelCommuteBookingCommandHandler,
    private val cancelAttractionBookingCommandHandler: CancelAttractionBookingCommandHandler,
    private val compensateCancelAccommodationBookingCommandHandler: CompensateCancelAccommodationBookingCommandHandler,
    private val compensateCancelCommuteBookingCommandHandler: CompensateCancelCommuteBookingCommandHandler,
    private val sagaState: SagaState,
    private val metadata: Metadata
) : PersistentSaga(sagaRepository, outboxPort, deadLetterQueueRepository, sagaState) {

    override suspend fun accommodationStep() {
        cancelAccommodationBookingCommandHandler.handle(
            CancelAccommodationBookingCommand(
                sagaState.travelOffer.accommodationId,
                metadata.correlationId,
                sagaState.bookingId,
            )
        )
    }

    override suspend fun compensateAccommodationStep() {
        compensateCancelAccommodationBookingCommandHandler.handle(
            CompensateCancelAccommodationBookingCommand(
                accommodationId = sagaState.travelOffer.accommodationId,
                correlationId = metadata.correlationId,
                bookingId = sagaState.bookingId,
            )
        )
    }

    override suspend fun commuteStep() {
        cancelCommuteBookingCommandHandler.handle(
            CancelCommuteBookingCommand(
                metadata.correlationId,
                sagaState.travelOffer.commuteId,
                sagaState.bookingId,
            )
        )
    }

    override suspend fun compensateCommuteStep() {
        compensateCancelCommuteBookingCommandHandler.handle(
            CompensateCancelCommuteBookingCommand(
                commuteId = sagaState.travelOffer.commuteId,
                correlationId = metadata.correlationId,
                bookingId = sagaState.bookingId,
                seat = sagaState.seat,
            )
        )
    }

    override suspend fun attractionStep() {
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> cancelAttractionBookingCommandHandler.handle(
                CancelAttractionBookingCommand(
                    attractionId,
                    metadata.correlationId,
                    sagaState.bookingId,
                )
            )
            is AttractionId.Empty -> return
        }
    }

    override fun getSagaStartedEvent(): SagaEvent =
        BookingCancelSagaStartedEvent(
            bookingId = sagaState.bookingId
        )

    override fun getSagaCompletedEvent(): SagaEvent =
        BookingCancelSagaCompletedEvent(
            bookingId = sagaState.bookingId,
            seat = sagaState.seat
        )

    override fun getSagaFailedEvent(): SagaEvent =
        BookingCancelSagaFailedEvent(
            bookingId = sagaState.bookingId,
            message = sagaState.message!!
        )
}
