package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface TravelOfferCommand : Command {
    val travelOfferId: UUID
}

data class BookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand

data class ReleaseTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand

data class RebookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
) : TravelOfferCommand

data class CancelBookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand

data class ReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand

data class CancelReserveTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val bookingId: UUID,
    val seat: Seat?,
) : TravelOfferCommand

data class CreateTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
) : TravelOfferCommand

data class ExpireTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand

data class MakeTravelOfferUnavailableCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand

data class MakeTravelOfferAvailableCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
) : TravelOfferCommand
