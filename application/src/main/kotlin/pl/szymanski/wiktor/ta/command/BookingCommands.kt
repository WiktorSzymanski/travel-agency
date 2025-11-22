package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed class BookingCommand : Command {
    abstract val bookingId: UUID
}

data class CreateBookingCommand(
    override val bookingId: UUID = UUID.randomUUID(),
    override val correlationId: UUID,
    val travelOfferId: UUID,
    val userId: UUID,
    val seat: Seat? = null,
) : BookingCommand()

data class ProcessBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class CompleteBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
) : BookingCommand()

data class CancelBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
) : BookingCommand()

data class FailBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class FailCancelBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class ProcessCancelBookingCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
) : BookingCommand()

data class BookingRequestCancelCommand(
    override val bookingId: UUID,
    override val correlationId: UUID,
) : BookingCommand()
