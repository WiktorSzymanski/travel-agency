package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationAndTime
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

sealed class CommuteCommand : Command {
    abstract val commuteId: CommuteId
}

data class BookCommuteCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CommuteCommand()

data class CancelCommuteBookingCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : CommuteCommand()

data class CreateCommuteCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
    val name: String,
    val departure: LocationAndTime,
    val arrival: LocationAndTime,
    val seats: List<Seat>,
) : CommuteCommand()

data class ExpireCommuteCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
) : CommuteCommand()

sealed class CompensateCommuteCommand : CommuteCommand() {
    abstract val eventId: UUID
}

data class CompensateBookCommuteCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
) : CompensateCommuteCommand()

data class CompensateCancelCommuteBookingCommand(
    override val commuteId: CommuteId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CompensateCommuteCommand()
