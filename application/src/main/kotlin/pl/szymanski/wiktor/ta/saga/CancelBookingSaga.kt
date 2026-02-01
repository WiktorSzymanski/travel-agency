package pl.szymanski.wiktor.ta.saga

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
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort

class CancelBookingSaga(
    commandBus: CommandBus,
    sagaRepository: SagaRepository,
    sagaOutboxPort: SagaOutboxPort,
    deadLetterQueueRepository: DeadLetterQueueRepository,
    private val sagaState: SagaState,
    private val metadata: Metadata
) : PersistentSaga(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository, sagaState) {
    override fun getAccommodationCommand(): AccommodationCommand =
        CancelAccommodationBookingCommand(
            sagaState.travelOffer.accommodationId,
            metadata.correlationId,
            sagaState.bookingId
        )

    override fun getCompensateAccommodationCommand(): CompensateAccommodationCommand =
        CompensateCancelAccommodationBookingCommand(
            accommodationId = sagaState.travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId
        )

    override fun getCommuteCommand(): CommuteCommand =
        CancelCommuteBookingCommand(
            sagaState.travelOffer.commuteId,
            metadata.correlationId,
            sagaState.bookingId,
        )

    override fun getCompensateCommuteCommand(): CompensateCommuteCommand =
        CompensateCancelCommuteBookingCommand(
            commuteId = sagaState.travelOffer.commuteId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId,
            seat = sagaState.seat,
        )

    override fun getAttractionCommand(): AttractionCommand? =
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> CancelAttractionBookingCommand(
                attractionId,
                metadata.correlationId,
                sagaState.bookingId
            )
            is AttractionId.Empty -> null
        }

    override fun getSagaStartedEvent(): EventEnvelope<SagaEvent> =
        EventEnvelope(BookingCancelSagaStartedEvent(
            bookingId = sagaState.bookingId
        ), metadata)

    override fun getSagaCompletedEvent(): EventEnvelope<SagaEvent> =
        EventEnvelope(BookingCancelSagaCompletedEvent(
            bookingId = sagaState.bookingId,
            seat = sagaState.seat),
            metadata)

    override fun getSagaFailedEvent(): EventEnvelope<SagaEvent> =
        EventEnvelope(BookingCancelSagaFailedEvent(
            bookingId = sagaState.bookingId,
            message = sagaState.message!!),
            metadata)

}
