package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.CommandBus
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
    init {
        CommandBus.registerHandler(FailBookingCommand::class.java) {
            this.handle(it as BookingCommand)
        }

        CommandBus.registerHandler(FailCancelBookingCommand::class.java) {
            this.handle(it as BookingCommand)
        }
    }

    suspend fun handle(command: BookingCommand): Pair<Booking, BookingEvent> =
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

    private fun handle(command: CreateBookingCommand): Pair<Booking, BookingEvent> =
        Booking.create(
            userId = command.userId,
            travelOfferId = command.travelOfferId,
            seat = command.seat,
        )

    private suspend fun handle(command: BookingRequestCancelCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.requestCancel()
                it to event
            }

    private suspend fun handle(command: ProcessBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.process()
                it to event
            }

    private suspend fun handle(command: CompleteBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.complete()
                it to event
            }

    private suspend fun handle(command: CancelBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.cancel()
                it to event
            }

    private suspend fun handle(command: FailBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.fail(command.message!!)
                it to event
            }

    private suspend fun handle(command: FailCancelBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.failCancellation(command.message!!)
                it to event
            }

    private suspend fun handle(command: ProcessCancelBookingCommand): Pair<Booking, BookingEvent> =
        bookingRepository
            .findById(command.bookingId)
            .let {
                val event = it.processCancellation()
                it to event
            }
}