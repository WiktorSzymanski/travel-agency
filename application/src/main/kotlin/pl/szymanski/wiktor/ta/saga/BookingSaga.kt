package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommand
import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommand
import pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommand
import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.outbox.OutboxPort

class BookingSaga(
    sagaRepository: SagaRepository,
    outboxPort: OutboxPort,
    deadLetterQueueRepository: DeadLetterQueueRepository,
    private val bookAccommodationCommandHandler: BookAccommodationCommandHandler,
    private val bookCommuteCommandHandler: BookCommuteCommandHandler,
    private val bookAttractionCommandHandler: BookAttractionCommandHandler,
    private val compensateBookAccommodationCommandHandler: CompensateBookAccommodationCommandHandler,
    private val compensateBookCommuteCommandHandler: CompensateBookCommuteCommandHandler,
    private val sagaState: SagaState,
    private val metadata: Metadata
) : PersistentSaga(sagaRepository, outboxPort, deadLetterQueueRepository, sagaState) {
    override suspend fun accommodationStep() {
        bookAccommodationCommandHandler.handle(
            BookAccommodationCommand(
                sagaState.travelOffer.accommodationId,
                metadata.correlationId,
                sagaState.bookingId,
            )
        )
    }

    override suspend fun compensateAccommodationStep() {
        compensateBookAccommodationCommandHandler.handle(
            CompensateBookAccommodationCommand(
                accommodationId = sagaState.travelOffer.accommodationId,
                correlationId = metadata.correlationId,
                bookingId = sagaState.bookingId,
            )
        )
    }

    override suspend fun commuteStep() {
        bookCommuteCommandHandler.handle(
                BookCommuteCommand(
                metadata.correlationId,
                sagaState.travelOffer.commuteId,
                sagaState.bookingId,
                sagaState.seat,
            )
        )
    }

    override suspend fun compensateCommuteStep() {
        compensateBookCommuteCommandHandler.handle(
            CompensateBookCommuteCommand(
                commuteId = sagaState.travelOffer.commuteId,
                correlationId = metadata.correlationId,
                bookingId = sagaState.bookingId,
            )
        )
    }

    override suspend fun attractionStep() {
        when (val attractionId = sagaState.travelOffer.attractionId) {
            is AttractionId.Present -> bookAttractionCommandHandler.handle(
                BookAttractionCommand(
                    attractionId,
                    metadata.correlationId,
                    sagaState.bookingId,
                )
            )
            is AttractionId.Empty -> return
        }
    }


    override fun getSagaStartedEvent(): SagaEvent =
        BookingSagaStartedEvent(
            bookingId = sagaState.bookingId.value!!
        )

    override fun getSagaCompletedEvent(): SagaEvent =
        BookingSagaCompletedEvent(
            bookingId = sagaState.bookingId.value!!,
            seat = sagaState.seat)

    override fun getSagaFailedEvent(): SagaEvent =
        BookingSagaFailedEvent(
            bookingId = sagaState.bookingId.value!!,
            message = sagaState.message!!)
}
