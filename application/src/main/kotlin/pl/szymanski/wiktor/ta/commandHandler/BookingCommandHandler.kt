package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.command.CreateBookingCommand
import pl.szymanski.wiktor.ta.command.UpdateBookingStateCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.BookingStateChangedEvent
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.withRetry

class BookingCommandHandler(
    private val bookingRepository: BookingRepository,
) {
    suspend fun handle(command: BookingCommand): BookingEvent =
        withRetry (3) {
            when (command) {
                is CreateBookingCommand -> handle(command)
                is UpdateBookingStateCommand -> handle(command)
                is BookingRequestCancelCommand -> handle(command)
            }
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    private suspend fun handle(command: CreateBookingCommand): BookingEvent =
        Booking.create(
            userId = command.userId,
            travelOfferId = command.travelOfferId,
            seat = command.seat,
        ).let { (booking, event) ->
            bookingRepository.save(booking)
            return event
        }

    private suspend fun handle(command: BookingRequestCancelCommand): BookingEvent =
        withRetry(3) {
            bookingRepository
                .findById(command.bookingId)
                .let { booking ->
                    booking
                        .requestCancel()
                        .also { bookingRepository.update(booking) }
                }
        }

    private suspend fun handle(command: UpdateBookingStateCommand): BookingEvent =
        withRetry(3) {
            bookingRepository
                .findById(command.bookingId)
                .let { booking ->
                    booking
                        .changeState(command.state, command.message)
                        .also { bookingRepository.update(booking) }
                }
        }

//    suspend fun compensate(event: BookingEvent): BookingEvent =
//        when (event) {
//            is BookingCreatedEvent -> {
//                // Delete the booking or mark it as CANCELED
//                bookingRepository.findById(event.bookingId).let { booking ->
//                    booking.changeState(pl.szymanski.wiktor.ta.domain.BookingState.CANCELED, "Compensated")
//                        .also { bookingRepository.update(booking) }
//                }
//            }
//            is BookingStateChangedEvent -> {
//                // Revert to the previous state if possible, or mark as CANCELED
//                bookingRepository.findById(event.bookingId).let { booking ->
//                    booking.changeState(pl.szymanski.wiktor.ta.domain.BookingState.CANCELED, "Compensated")
//                        .also { bookingRepository.update(booking) }
//                }
//            }
//            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
//        }.apply { correlationId = event.correlationId }.also { EventBus.publish(it) }
}