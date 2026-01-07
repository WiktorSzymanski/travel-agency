package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository

class BookingCommandHandler(
    private val bookingRepository: BookingRepository,
) {
    suspend fun handle(command: BookingCommand): Pair<Booking, List<BookingEvent>> =
        when (command) {
            is CreateBookingCommand -> handle(command)
            is ProcessBookingCommand -> handle(command)
            is CompleteBookingCommand -> handle(command)
            is CancelBookingCommand -> handle(command)
            is FailBookingCommand -> handle(command)
            is FailCancelBookingCommand -> handle(command)
            is BookingRequestCancelCommand -> handle(command)
            is ProcessCancelBookingCommand -> handle(command)
        }

    private fun handle(command: CreateBookingCommand): Pair<Booking, List<BookingEvent>> =
        Booking.create(
            userId = command.userId,
            travelOffer = command.travelOffer,
            seat = command.seat,
        )

    private suspend fun handle(command: BookingRequestCancelCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.requestCancel()
                it to events
            }

    private suspend fun handle(command: ProcessBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.process()
                it to events
            }

    private suspend fun handle(command: CompleteBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.complete()
                it to events
            }

    private suspend fun handle(command: CancelBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.cancel()
                it to events
            }

    private suspend fun handle(command: FailBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.fail(command.message!!)
                it to events
            }

    private suspend fun handle(command: FailCancelBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.failCancellation(command.message!!)
                it to events
            }

    private suspend fun handle(command: ProcessCancelBookingCommand): Pair<Booking, List<BookingEvent>> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.processCancellation()
                it to events
            }
}
