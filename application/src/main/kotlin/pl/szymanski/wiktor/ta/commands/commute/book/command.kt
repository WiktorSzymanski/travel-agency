package pl.szymanski.wiktor.ta.commands.commute.book

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

data class BookCommuteCommand(
    override val correlationId: UUID,
    val commuteId: CommuteId,
    val bookingId: BookingId,
    val seat: Seat,
) : Command
