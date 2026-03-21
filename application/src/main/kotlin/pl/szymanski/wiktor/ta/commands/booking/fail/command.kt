package pl.szymanski.wiktor.ta.commands.booking.fail

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class FailBookingCommand(
    override val correlationId: UUID,
    val bookingId: BookingId,
    val message: String? = null,
) : Command
