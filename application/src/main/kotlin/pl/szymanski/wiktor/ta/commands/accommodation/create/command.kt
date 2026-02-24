package pl.szymanski.wiktor.ta.commands.accommodation.create

import pl.szymanski.wiktor.ta.commands.Command
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import java.util.UUID

data class CreateAccommodationCommand(
    val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : Command
