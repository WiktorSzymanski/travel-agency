package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.util.UUID

sealed class AccommodationCommand : Command {
    abstract val accommodationId: UUID
}

data class BookAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : AccommodationCommand()

data class CancelAccommodationBookingCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : AccommodationCommand()

data class CreateAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationCommand()

data class ExpireAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
) : AccommodationCommand()
