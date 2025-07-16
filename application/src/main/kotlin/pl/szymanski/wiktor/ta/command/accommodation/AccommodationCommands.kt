package pl.szymanski.wiktor.ta.command.accommodation

import pl.szymanski.wiktor.ta.command.Command
import java.util.UUID

sealed interface AccommodationCommand: Command {
    val accommodationId: UUID
}

data class BookAccommodationCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val userId: UUID
) : AccommodationCommand

data class CancelAccommodationBookingCommand(
    override val accommodationId: UUID,
    override val correlationId: UUID,
    val userId: UUID
) : AccommodationCommand