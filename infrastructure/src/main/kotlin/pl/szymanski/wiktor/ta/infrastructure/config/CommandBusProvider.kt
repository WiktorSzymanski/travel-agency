package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.create.CreateAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.create.CreateAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.compensateBook.CompensateBookAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.compensateCancelBooking.CompensateCancelAttractionBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.create.CreateCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.repository.AttractionRepository
import pl.szymanski.wiktor.ta.repository.BookingRepository
import pl.szymanski.wiktor.ta.repository.CommuteRepository

@Configuration
class CommandHandlerConfiguration {
    @Bean fun createAccommodationCommandHandler(r: AccommodationRepository, o: OutboxPort) = CreateAccommodationCommandHandler(r, o)
    @Bean fun bookAccommodationCommandHandler(r: AccommodationRepository, o: OutboxPort) = BookAccommodationCommandHandler(r, o)
    @Bean fun cancelAccommodationBookingCommandHandler(r: AccommodationRepository, o: OutboxPort) = CancelAccommodationBookingCommandHandler(r, o)
    @Bean fun expireAccommodationCommandHandler(r: AccommodationRepository, o: OutboxPort) = ExpireAccommodationCommandHandler(r, o)
    @Bean fun compensateBookAccommodationCommandHandler(r: AccommodationRepository, o: OutboxPort) = CompensateBookAccommodationCommandHandler(r, o)
    @Bean fun compensateCancelAccommodationBookingCommandHandler(r: AccommodationRepository, o: OutboxPort) = CompensateCancelAccommodationBookingCommandHandler(r, o)

    @Bean fun createAttractionCommandHandler(r: AttractionRepository, o: OutboxPort) = CreateAttractionCommandHandler(r, o)
    @Bean fun bookAttractionCommandHandler(r: AttractionRepository, o: OutboxPort) = BookAttractionCommandHandler(r, o)
    @Bean fun cancelAttractionBookingCommandHandler(r: AttractionRepository, o: OutboxPort) = CancelAttractionBookingCommandHandler(r, o)
    @Bean fun expireAttractionCommandHandler(r: AttractionRepository, o: OutboxPort) = ExpireAttractionCommandHandler(r, o)
    @Bean fun compensateBookAttractionCommandHandler(r: AttractionRepository, o: OutboxPort) = CompensateBookAttractionCommandHandler(r, o)
    @Bean fun compensateCancelAttractionBookingCommandHandler(r: AttractionRepository, o: OutboxPort) = CompensateCancelAttractionBookingCommandHandler(r, o)

    @Bean fun createCommuteCommandHandler(r: CommuteRepository, o: OutboxPort) = CreateCommuteCommandHandler(r, o)
    @Bean fun bookCommuteCommandHandler(r: CommuteRepository, o: OutboxPort) = BookCommuteCommandHandler(r, o)
    @Bean fun cancelCommuteBookingCommandHandler(r: CommuteRepository, o: OutboxPort) = CancelCommuteBookingCommandHandler(r, o)
    @Bean fun expireCommuteCommandHandler(r: CommuteRepository, o: OutboxPort) = ExpireCommuteCommandHandler(r, o)
    @Bean fun compensateBookCommuteCommandHandler(r: CommuteRepository, o: OutboxPort) = CompensateBookCommuteCommandHandler(r, o)
    @Bean fun compensateCancelCommuteBookingCommandHandler(r: CommuteRepository, o: OutboxPort) = CompensateCancelCommuteBookingCommandHandler(r, o)

    @Bean fun createBookingCommandHandler(r: BookingRepository, o: OutboxPort) = CreateBookingCommandHandler(r, o)
    @Bean fun processBookingCommandHandler(r: BookingRepository, o: OutboxPort) = ProcessBookingCommandHandler(r, o)
    @Bean fun completeBookingCommandHandler(r: BookingRepository, o: OutboxPort) = CompleteBookingCommandHandler(r, o)
    @Bean fun cancelBookingCommandHandler(r: BookingRepository, o: OutboxPort) = CancelBookingCommandHandler(r, o)
    @Bean fun failBookingCommandHandler(r: BookingRepository, o: OutboxPort) = FailBookingCommandHandler(r, o)
    @Bean fun failCancelBookingCommandHandler(r: BookingRepository, o: OutboxPort) = FailCancelBookingCommandHandler(r, o)
    @Bean fun processCancelBookingCommandHandler(r: BookingRepository, o: OutboxPort) = ProcessCancelBookingCommandHandler(r, o)
    @Bean fun bookingRequestCancelCommandHandler(r: BookingRepository, o: OutboxPort) = BookingRequestCancelCommandHandler(r, o)
}
