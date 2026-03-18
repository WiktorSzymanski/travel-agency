package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.outbox.OutboxPort

class SagaService(
    private val sagaRepository: SagaRepository,
    private val outboxPort: OutboxPort,
    private val deadLetterQueueRepository: DeadLetterQueueRepository,
    private val bookAccommodationCommandHandler: BookAccommodationCommandHandler,
    private val bookCommuteCommandHandler: BookCommuteCommandHandler,
    private val bookAttractionCommandHandler: BookAttractionCommandHandler,
    private val compensateBookAccommodationCommandHandler: CompensateBookAccommodationCommandHandler,
    private val compensateBookCommuteCommandHandler: CompensateBookCommuteCommandHandler,
    private val cancelAccommodationBookingCommandHandler: CancelAccommodationBookingCommandHandler,
    private val cancelCommuteBookingCommandHandler: CancelCommuteBookingCommandHandler,
    private val cancelAttractionBookingCommandHandler: CancelAttractionBookingCommandHandler,
    private val compensateCancelAccommodationBookingCommandHandler: CompensateCancelAccommodationBookingCommandHandler,
    private val compensateCancelCommuteBookingCommandHandler: CompensateCancelCommuteBookingCommandHandler,
) {
    suspend fun executeSaga(sagaState: SagaState) = getSagaInstance(sagaState).executeOrResume()

    suspend fun executePendingSagas() { // TODO: SagaPoller?
        val pendingSagas =
            sagaRepository.findByStatuses(listOf(SagaStatus.NEW, SagaStatus.PROCESSING))

        pendingSagas
            .forEach { getSagaInstance(it).executeOrResume() }
    }

    private fun getSagaInstance(sagaState: SagaState) = when(sagaState.type) {
        SagaType.BOOKING -> BookingSaga(
            sagaRepository = sagaRepository,
            outboxPort = outboxPort,
            deadLetterQueueRepository = deadLetterQueueRepository,
            bookAccommodationCommandHandler = bookAccommodationCommandHandler,
            bookCommuteCommandHandler = bookCommuteCommandHandler,
            bookAttractionCommandHandler = bookAttractionCommandHandler,
            compensateBookAccommodationCommandHandler = compensateBookAccommodationCommandHandler,
            compensateBookCommuteCommandHandler = compensateBookCommuteCommandHandler,
            sagaState = sagaState,
            metadata = Metadata(sagaState.correlationId, sagaState.version.toLong())
        )
        SagaType.CANCELLING -> CancelBookingSaga(
            sagaRepository = sagaRepository,
            outboxPort = outboxPort,
            deadLetterQueueRepository = deadLetterQueueRepository,
            cancelAccommodationBookingCommandHandler = cancelAccommodationBookingCommandHandler,
            cancelCommuteBookingCommandHandler = cancelCommuteBookingCommandHandler,
            cancelAttractionBookingCommandHandler = cancelAttractionBookingCommandHandler,
            compensateCancelAccommodationBookingCommandHandler = compensateCancelAccommodationBookingCommandHandler,
            compensateCancelCommuteBookingCommandHandler = compensateCancelCommuteBookingCommandHandler,
            sagaState = sagaState,
            metadata = Metadata(sagaState.correlationId, sagaState.version.toLong())
        )
    }
}
