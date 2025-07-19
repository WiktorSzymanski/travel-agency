package pl.szymanski.wiktor.ta.command.commute

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface CommuteCommand : Command {
    val commuteId: UUID
}

data class BookCommuteCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteCommand

data class CancelCommuteBookingCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteCommand
