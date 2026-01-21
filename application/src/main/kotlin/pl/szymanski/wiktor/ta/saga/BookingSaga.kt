package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.CommandBus
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
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.outbox.SagaOutboxPort

class BookingSaga(
    commandBus: CommandBus,
    sagaRepository: SagaRepository,
    sagaOutboxPort: SagaOutboxPort,
    deadLetterQueueRepository: DeadLetterQueueRepository,
    private val sagaState: SagaState,
    private val metadata: Metadata
) : PersistentSaga(commandBus, sagaRepository, sagaOutboxPort, deadLetterQueueRepository, sagaState) {
    override fun getAccommodationCommand(): AccommodationCommand =
        BookAccommodationCommand(
            sagaState.travelOffer.accommodationId,
            metadata.correlationId,
            sagaState.bookingId,
        )

    override fun getCompensateAccommodationCommand(): CompensateAccommodationCommand =
        CompensateBookAccommodationCommand(
            accommodationId = sagaState.travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId,
        )

    override fun getCommuteCommand(): CommuteCommand =
        BookCommuteCommand(
            sagaState.travelOffer.commuteId,
            metadata.correlationId,
            sagaState.bookingId,
            sagaState.seat,
        )

    override fun getCompensateCommuteCommand(): CompensateCommuteCommand =
        CompensateBookCommuteCommand(
            commuteId = sagaState.travelOffer.commuteId,
            correlationId = metadata.correlationId,
            bookingId = sagaState.bookingId,
        )

    override fun getAttractionCommand(): AttractionCommand? =
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> BookAttractionCommand(
                attractionId,
                metadata.correlationId,
                sagaState.bookingId,
            )
            is AttractionId.Empty -> null
        }

    override fun getSagaStartedEvent(): EventEnvelope<SagaEvent> = EventEnvelope(
        BookingSagaStartedEvent(
            bookingId = sagaState.bookingId
        ), metadata
    )

    override fun getSagaCompletedEvent(): EventEnvelope<SagaEvent> =
        EventEnvelope(BookingSagaCompletedEvent(
            bookingId = sagaState.bookingId,
            seat = sagaState.seat),
            metadata)

    override fun getSagaFailedEvent(): EventEnvelope<SagaEvent> =
        EventEnvelope(BookingSagaFailedEvent(
            bookingId = sagaState.bookingId,
            message = sagaState.message!!),
            metadata)
}
