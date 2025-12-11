package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed class CommuteCommand : Command {
    abstract val commuteId: UUID
}

data class BookCommuteCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat,
) : CommuteCommand()

data class CancelCommuteBookingCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : CommuteCommand()

data class CreateCommuteCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteCommand()

data class ExpireCommuteCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
) : CommuteCommand()

sealed class CompensateCommuteCommand : CommuteCommand() {
    abstract val eventId: UUID
}

data class CompensateBookCommuteCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
) : CompensateCommuteCommand()

data class CompensateCancelCommuteBookingCommand(
    override val commuteId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
    val seat: Seat,
) : CompensateCommuteCommand()
