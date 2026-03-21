package pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

data class CompensateCancelCommuteBookingCommand(
    override val correlationId: UUID,
    val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : Command
