package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
import pl.szymanski.wiktor.ta.domain.aggregate.*
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.service.*

@Configuration
class ServiceConfiguration {
    @Bean
    fun accommodationService(r: AccommodationQueryRepository) = AccommodationService(r)

    @Bean
    fun commuteService(r: CommuteQueryRepository) = CommuteService(r)

    @Bean
    fun attractionService(r: AttractionQueryRepository) = AttractionService(r)

    @Bean
    fun bookingService(
        createBookingCommandHandler: CreateBookingCommandHandler,
        completeBookingCommandHandler: CompleteBookingCommandHandler,
        cancelBookingCommandHandler: CancelBookingCommandHandler,
        failBookingCommandHandler: FailBookingCommandHandler,
        failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
        bookingRequestCancelCommandHandler: BookingRequestCancelCommandHandler,
        accommodationService: AccommodationService,
        commuteService: CommuteService,
        attractionService: AttractionService,
    ): BookingService = BookingService(
        createBookingCommandHandler = createBookingCommandHandler,
        completeBookingCommandHandler = completeBookingCommandHandler,
        cancelBookingCommandHandler = cancelBookingCommandHandler,
        failBookingCommandHandler = failBookingCommandHandler,
        failCancelBookingCommandHandler = failCancelBookingCommandHandler,
        bookingRequestCancelCommandHandler = bookingRequestCancelCommandHandler,
        accommodationService = accommodationService,
        commuteService = commuteService,
        attractionService = attractionService,
    )
}