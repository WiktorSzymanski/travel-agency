package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.Metadata
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
import pl.szymanski.wiktor.ta.repository.BookingRepository
import java.util.UUID

class BookingCommandHandler(
    private val bookingRepository: BookingRepository,
) {
    suspend fun handle(command: BookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
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

    private fun handle(command: CreateBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        Booking.create(
            userId = command.userId,
            travelOffer = command.travelOffer,
            seat = command.seat,
        ).let { Triple(it.first, it.second, Metadata(command.correlationId, 0)) }

    private suspend fun handle(command: BookingRequestCancelCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.requestCancel()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: ProcessBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.process()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: CompleteBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.complete()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: CancelBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.cancel()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: FailBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.fail(command.message!!)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: FailCancelBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.failCancellation(command.message!!)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: ProcessCancelBookingCommand): Triple<Booking, List<BookingEvent>, Metadata> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val events = it.first.processCancellation()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }
}
