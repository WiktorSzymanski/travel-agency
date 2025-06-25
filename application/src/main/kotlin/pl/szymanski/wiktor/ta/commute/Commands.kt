package pl.szymanski.wiktor.ta.commute

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface CommuteCommand {
    val commuteId: UUID
}

data class CreateCommuteCommand(
    override val commuteId: UUID,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteCommand

data class CancelCommuteCommand(
    override val commuteId: UUID,
) : CommuteCommand

data class CommuteDepartureCommand(
    override val commuteId: UUID,
) : CommuteCommand

data class CommuteArrivalCommand(
    override val commuteId: UUID,
) : CommuteCommand

data class BookSeatCommand(
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteCommand

data class CancelBookedSeatCommand(
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteCommand
