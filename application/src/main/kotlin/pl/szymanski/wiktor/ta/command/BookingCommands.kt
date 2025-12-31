package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.util.UUID

sealed class BookingCommand : Command {
    abstract val bookingId: BookingId
}

data class CreateBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
    val travelOfferId: TravelOfferId,
    val userId: UUID,
    val seat: Seat
) : BookingCommand()

data class ProcessBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class CompleteBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
) : BookingCommand()

data class CancelBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
) : BookingCommand()

data class FailBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class FailCancelBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
    val message: String? = null,
) : BookingCommand()

data class ProcessCancelBookingCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
) : BookingCommand()

data class BookingRequestCancelCommand(
    override val bookingId: BookingId,
    override val correlationId: UUID,
) : BookingCommand()
