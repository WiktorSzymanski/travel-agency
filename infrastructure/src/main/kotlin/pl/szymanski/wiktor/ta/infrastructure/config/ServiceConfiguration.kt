package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
import pl.szymanski.wiktor.ta.service.BookingService

@Configuration
class ServiceConfiguration {
    @Bean
    fun bookingService(
        createBookingCommandHandler: CreateBookingCommandHandler,
        processBookingCommandHandler: ProcessBookingCommandHandler,
        completeBookingCommandHandler: CompleteBookingCommandHandler,
        cancelBookingCommandHandler: CancelBookingCommandHandler,
        failBookingCommandHandler: FailBookingCommandHandler,
        failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
        processCancelBookingCommandHandler: ProcessCancelBookingCommandHandler,
        bookingRequestCancelCommandHandler: BookingRequestCancelCommandHandler,
    ): BookingService = BookingService(
        createBookingCommandHandler = createBookingCommandHandler,
        processBookingCommandHandler = processBookingCommandHandler,
        completeBookingCommandHandler = completeBookingCommandHandler,
        cancelBookingCommandHandler = cancelBookingCommandHandler,
        failBookingCommandHandler = failBookingCommandHandler,
        failCancelBookingCommandHandler = failCancelBookingCommandHandler,
        processCancelBookingCommandHandler = processCancelBookingCommandHandler,
        bookingRequestCancelCommandHandler = bookingRequestCancelCommandHandler,
    )
}