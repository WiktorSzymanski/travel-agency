package pl.szymanski.wiktor.ta.commands.booking.create

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

data class CreateBookingCommand(
    override val correlationId: UUID,
    val bookingId: BookingId = BookingId.generate(),
    val travelOffer: TravelOffer,
    val userId: UUID,
    val seat: Seat,
) : Command
