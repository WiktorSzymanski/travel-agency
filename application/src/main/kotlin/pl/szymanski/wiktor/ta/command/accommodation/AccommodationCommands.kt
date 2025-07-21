package pl.szymanski.wiktor.ta.command.accommodation

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.util.UUID

sealed interface AccommodationCommand : Command {
    val accommodationId: UUID
}

data class BookAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
) : AccommodationCommand

data class CancelAccommodationBookingCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
) : AccommodationCommand

data class CreateAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationCommand

data class ExpireAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
) : AccommodationCommand
