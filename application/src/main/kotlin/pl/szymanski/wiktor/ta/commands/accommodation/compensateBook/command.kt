package pl.szymanski.wiktor.ta.commands.accommodation.compensateBook

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

data class CompensateBookAccommodationCommand(
    val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : Command
