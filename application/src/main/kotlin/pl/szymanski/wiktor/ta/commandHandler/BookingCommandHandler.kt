package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.command.UpdateBookingStateCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.BookingStateChangedEvent
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository

class BookingCommandHandler(
    private val bookingRepository: BookingRepository,
) {
    suspend fun handle(command: BookingCommand): BookingEvent =
        when (command) {
            is CreateBookingCommand -> handle(command)
            is UpdateBookingStateCommand -> handle(command)
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    suspend fun handle(command: CreateBookingCommand): BookingEvent =
        Booking.create(
            userId = command.userId,
            travelOfferId = command.travelOfferId,
            seat = command.seat,
        ).let { (booking, event) ->
            // If a specific bookingId was provided in the command, use it
            val finalBooking = if (command.bookingId != booking._id) {
                booking.copy(_id = command.bookingId)
            } else {
                booking
            }
            
            bookingRepository.save(finalBooking)
            
            // Update the event with the correct bookingId
            event.copy(bookingId = finalBooking._id)
        }

    suspend fun handle(command: UpdateBookingStateCommand): BookingEvent =
        bookingRepository
            .findById(command.bookingId)
            .let { booking ->
                booking
                    .changeState(command.state, command.message)
                    .also { bookingRepository.update(booking) }
            }.apply { correlationId = command.correlationId }

    suspend fun compensate(event: BookingEvent): BookingEvent =
        when (event) {
            is BookingCreatedEvent -> {
                // Delete the booking or mark it as CANCELED
                bookingRepository.findById(event.bookingId).let { booking ->
                    booking.changeState(pl.szymanski.wiktor.ta.domain.BookingState.CANCELED, "Compensated")
                        .also { bookingRepository.update(booking) }
                }
            }
            is BookingStateChangedEvent -> {
                // Revert to the previous state if possible, or mark as CANCELED
                bookingRepository.findById(event.bookingId).let { booking ->
                    booking.changeState(pl.szymanski.wiktor.ta.domain.BookingState.CANCELED, "Compensated")
                        .also { bookingRepository.update(booking) }
                }
            }
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.also { EventBus.publish(it) }
}