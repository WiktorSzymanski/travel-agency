package pl.szymanski.wiktor.ta.command

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.util.UUID

sealed class TravelOfferCommand : Command {
    abstract val travelOfferId: TravelOfferId
}

data class BookTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferCommand()

data class ReleaseTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferCommand()

data class RebookTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
) : TravelOfferCommand()

data class CancelBookTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferCommand()

data class ReserveTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferCommand()

data class CancelReserveTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferCommand()

data class CreateTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    val name: String,
    val commuteId: CommuteId,
    val accommodationId: AccommodationId,
    val attractionId: AttractionId
) : TravelOfferCommand()

data class ExpireTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
) : TravelOfferCommand()

data class MakeTravelOfferUnavailableCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
) : TravelOfferCommand()

data class MakeTravelOfferAvailableCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
) : TravelOfferCommand()

sealed class CompensateTravelOfferCommand : TravelOfferCommand() {
    abstract val eventId: UUID
}

data class CompensateBookTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CompensateTravelOfferCommand()

data class CompensateCancelBookTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CompensateTravelOfferCommand()

data class CompensateReserveTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CompensateTravelOfferCommand()

data class CompensateCancelReserveTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
    val seat: Seat,
) : CompensateTravelOfferCommand()

data class CompensateReleaseTravelOfferCommand(
    override val travelOfferId: TravelOfferId,
    override val correlationId: UUID,
    override val eventId: UUID,
    val bookingId: BookingId,
) : CompensateTravelOfferCommand()
