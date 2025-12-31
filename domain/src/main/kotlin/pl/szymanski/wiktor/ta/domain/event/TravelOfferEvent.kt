package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import java.util.UUID

sealed interface TravelOfferEvent : DomainEvent {
    val travelOfferId: TravelOfferId
}

data class TravelOfferCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val name: String,
    val commuteId: CommuteId,
    val accommodationId: AccommodationId,
    val attractionId: AttractionId = AttractionId.Empty,
) : TravelOfferEvent

data class TravelOfferReservedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferReservationCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferReleaseEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferRebookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val bookingId: BookingId,
) : TravelOfferEvent

data class TravelOfferExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
) : TravelOfferEvent

data class TravelOfferMadeUnavailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
) : TravelOfferEvent

data class TravelOfferMadeAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
) : TravelOfferEvent

data class TravelOfferBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val travelOfferId: TravelOfferId,
    val accommodationId: AccommodationId,
    val commuteId: CommuteId,
    val attractionId: AttractionId,
    val bookingId: BookingId,
    val seat: Seat,
) : TravelOfferEvent