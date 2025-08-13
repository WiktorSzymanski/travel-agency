package pl.szymanski.wiktor.ta.domain.event

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

data class TravelOfferCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
) : TravelOfferEvent

data class TravelOfferReservedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val bookingId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferReservationCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val bookingId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val bookingId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferReleaseEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val bookingId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookingCanceledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val bookingId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferRebookedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
) : TravelOfferEvent

data class TravelOfferExpiredEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
) : TravelOfferEvent

data class TravelOfferMadeUnavailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
) : TravelOfferEvent

data class TravelOfferMadeAvailableEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
) : TravelOfferEvent

data class TravelOfferReserveFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferBookFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferMakeUnavailableFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferMakeAvailableFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferExpireFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferReservationCancelFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferBookingCancelFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferReleaseCompleteFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent

data class TravelOfferRebookCompleteFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID? = null,
    override val travelOfferId: UUID,
    val bookingId: UUID,
    override val message: String,
) : TravelOfferFailedEvent