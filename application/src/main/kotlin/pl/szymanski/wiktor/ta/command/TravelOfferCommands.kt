package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed class TravelOfferCommand : Command {
    abstract val travelOfferId: UUID
}

data class BookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand()

data class ReleaseTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand()

data class RebookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : TravelOfferCommand()

data class CancelBookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand()

data class ReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat? = null,
) : TravelOfferCommand()

data class CancelReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand()

data class CreateTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
) : TravelOfferCommand()

data class ExpireTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand()

data class MakeTravelOfferUnavailableCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand()

data class MakeTravelOfferAvailableCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand()

sealed class CompensateTravelOfferCommand : TravelOfferCommand() {
    abstract val eventId: UUID
}

data class CompensateBookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : CompensateTravelOfferCommand()

data class CompensateCancelBookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : CompensateTravelOfferCommand()

data class CompensateReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : CompensateTravelOfferCommand()

data class CompensateCancelReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : CompensateTravelOfferCommand()

data class CompensateReleaseTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: UUID,
) : CompensateTravelOfferCommand()
