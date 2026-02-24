package pl.szymanski.wiktor.ta.commands.booking.processCancel

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class ProcessCancelBookingCommand(
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
