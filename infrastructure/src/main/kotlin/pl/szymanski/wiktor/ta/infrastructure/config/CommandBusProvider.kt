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
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.outbox.OutboxPort
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

@Configuration
class CommandHandlerConfiguration {
    @Bean fun createAccommodationCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = CreateAccommodationCommandHandler(r, o)
    @Bean fun bookAccommodationCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = BookAccommodationCommandHandler(r, o)
    @Bean fun cancelAccommodationBookingCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = CancelAccommodationBookingCommandHandler(r, o)
    @Bean fun expireAccommodationCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = ExpireAccommodationCommandHandler(r, o)
    @Bean fun compensateBookAccommodationCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = CompensateBookAccommodationCommandHandler(r, o)
    @Bean fun compensateCancelAccommodationBookingCommandHandler(r: CommandRepository<Accommodation, AccommodationId>, o: OutboxPort) = CompensateCancelAccommodationBookingCommandHandler(r, o)

    @Bean fun createAttractionCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = CreateAttractionCommandHandler(r, o)
    @Bean fun bookAttractionCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = BookAttractionCommandHandler(r, o)
    @Bean fun cancelAttractionBookingCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = CancelAttractionBookingCommandHandler(r, o)
    @Bean fun expireAttractionCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = ExpireAttractionCommandHandler(r, o)
    @Bean fun compensateBookAttractionCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = CompensateBookAttractionCommandHandler(r, o)
    @Bean fun compensateCancelAttractionBookingCommandHandler(r: CommandRepository<Attraction, AttractionId>, o: OutboxPort) = CompensateCancelAttractionBookingCommandHandler(r, o)

    @Bean fun createCommuteCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = CreateCommuteCommandHandler(r, o)
    @Bean fun bookCommuteCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = BookCommuteCommandHandler(r, o)
    @Bean fun cancelCommuteBookingCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = CancelCommuteBookingCommandHandler(r, o)
    @Bean fun expireCommuteCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = ExpireCommuteCommandHandler(r, o)
    @Bean fun compensateBookCommuteCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = CompensateBookCommuteCommandHandler(r, o)
    @Bean fun compensateCancelCommuteBookingCommandHandler(r: CommandRepository<Commute, CommuteId>, o: OutboxPort) = CompensateCancelCommuteBookingCommandHandler(r, o)

    @Bean fun createBookingCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = CreateBookingCommandHandler(r, o)
    @Bean fun completeBookingCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = CompleteBookingCommandHandler(r, o)
    @Bean fun cancelBookingCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = CancelBookingCommandHandler(r, o)
    @Bean fun failBookingCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = FailBookingCommandHandler(r, o)
    @Bean fun failCancelBookingCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = FailCancelBookingCommandHandler(r, o)
    @Bean fun bookingRequestCancelCommandHandler(r: CommandRepository<Booking, BookingId>, o: OutboxPort) = BookingRequestCancelCommandHandler(r, o)
}
