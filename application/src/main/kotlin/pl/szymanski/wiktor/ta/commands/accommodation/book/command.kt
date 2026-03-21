package pl.szymanski.wiktor.ta.commands.accommodation.book

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class BookAccommodationCommand(
    val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
