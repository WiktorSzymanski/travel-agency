package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import pl.szymanski.wiktor.ta.saga.SagaRepository
import pl.szymanski.wiktor.ta.saga.SagaService

@Configuration
class SagaConfiguration {
    @Bean
    fun sagaService(
        sagaRepository: SagaRepository,
        outboxPort: OutboxPort,
        deadLetterQueueRepository: DeadLetterQueueRepository,
        bookAccommodationCommandHandler: BookAccommodationCommandHandler,
        bookCommuteCommandHandler: BookCommuteCommandHandler,
        bookAttractionCommandHandler: BookAttractionCommandHandler,
        compensateBookAccommodationCommandHandler: CompensateBookAccommodationCommandHandler,
        compensateBookCommuteCommandHandler: CompensateBookCommuteCommandHandler,
        cancelAccommodationBookingCommandHandler: CancelAccommodationBookingCommandHandler,
        cancelCommuteBookingCommandHandler: CancelCommuteBookingCommandHandler,
        cancelAttractionBookingCommandHandler: CancelAttractionBookingCommandHandler,
        compensateCancelAccommodationBookingCommandHandler: CompensateCancelAccommodationBookingCommandHandler,
        compensateCancelCommuteBookingCommandHandler: CompensateCancelCommuteBookingCommandHandler
    ) = SagaService(
        sagaRepository,
        outboxPort,
        deadLetterQueueRepository,
        bookAccommodationCommandHandler,
        bookCommuteCommandHandler,
        bookAttractionCommandHandler,
        compensateBookAccommodationCommandHandler,
        compensateBookCommuteCommandHandler,
        cancelAccommodationBookingCommandHandler,
        cancelCommuteBookingCommandHandler,
        cancelAttractionBookingCommandHandler,
        compensateCancelAccommodationBookingCommandHandler,
        compensateCancelCommuteBookingCommandHandler
    )
}