package pl.szymanski.wiktor.ta.commands.commute.cancelBooking

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

data class CancelCommuteBookingCommand(
    override val correlationId: UUID,
    val commuteId: CommuteId,
    val bookingId: BookingId,
) : Command
