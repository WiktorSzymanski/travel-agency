package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.util.UUID

sealed class AccommodationCommand : Command {
    abstract val accommodationId: AccommodationId
}

data class BookAccommodationCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : AccommodationCommand()

data class CancelAccommodationBookingCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : AccommodationCommand()

data class CreateAccommodationCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
) : AccommodationCommand()

data class ExpireAccommodationCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
) : AccommodationCommand()

sealed class CompensateAccommodationCommand : AccommodationCommand()

data class CompensateBookAccommodationCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : CompensateAccommodationCommand()

data class CompensateCancelAccommodationBookingCommand(
    override val accommodationId: AccommodationId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : CompensateAccommodationCommand()
