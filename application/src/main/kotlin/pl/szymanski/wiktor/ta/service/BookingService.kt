package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommand
import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

class BookingService(
    private val createBookingCommandHandler: CreateBookingCommandHandler,
    private val completeBookingCommandHandler: CompleteBookingCommandHandler,
    private val cancelBookingCommandHandler: CancelBookingCommandHandler,
    private val failBookingCommandHandler: FailBookingCommandHandler,
    private val failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
    private val bookingRequestCancelCommandHandler: BookingRequestCancelCommandHandler,
) {
    suspend fun createBooking(userId: UUID, travelOffer: TravelOffer, seat: Seat): BookingId {
        val bookingId = BookingId.generate()
        createBookingCommandHandler.handle(
            CreateBookingCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
                travelOffer = travelOffer,
                userId = userId,
                seat = seat,
            )
        )

        return bookingId
    }

    suspend fun completeBooking(bookingId: BookingId) {
        completeBookingCommandHandler.handle(
            CompleteBookingCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
            )
        )
    }

    suspend fun cancelBooking(bookingId: BookingId) {
        cancelBookingCommandHandler.handle(
            CancelBookingCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
            )
        )
    }

    suspend fun failBooking(bookingId: BookingId, message: String) {
        failBookingCommandHandler.handle(
            FailBookingCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
                message = message,
            )
        )
    }

    suspend fun failCancelBooking(bookingId: BookingId, message: String) {
        failCancelBookingCommandHandler.handle(
            FailCancelBookingCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
                message = message,
            )
        )
    }

    suspend fun requestCancelBooking(bookingId: BookingId) {
        bookingRequestCancelCommandHandler.handle(
            BookingRequestCancelCommand(
                bookingId = bookingId,
                correlationId = UUID.randomUUID(),
            )
        )
    }
}