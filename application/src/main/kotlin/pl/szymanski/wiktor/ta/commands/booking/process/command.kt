package pl.szymanski.wiktor.ta.commands.booking.process

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class ProcessBookingCommand(
    override val correlationId: UUID,
    val bookingId: BookingId,
    val message: String? = null,
) : Command
