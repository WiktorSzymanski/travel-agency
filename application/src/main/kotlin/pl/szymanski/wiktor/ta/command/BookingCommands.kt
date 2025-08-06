package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface BookingCommand : Command {
    val bookingId: UUID
}

data class CreateBookingCommand(
    override val bookingId: UUID = UUID.randomUUID(),
    override val correlationId: UUID,
    val travelOfferId: UUID,
    val userId: UUID,
    val seat: Seat,
) : BookingCommand

data class UpdateBookingStateCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
    val state: BookingState,
    val message: String? = null,
) : BookingCommand