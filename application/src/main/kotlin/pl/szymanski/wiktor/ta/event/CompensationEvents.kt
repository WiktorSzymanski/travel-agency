package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import java.util.UUID

data class AccommodationBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val accommodationId: UUID,
    val userId: UUID,
) : AccommodationEvent

data class AccommodationBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val accommodationId: UUID,
    val userId: UUID,
) : AccommodationEvent

data class AttractionBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent

data class AttractionBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val attractionId: UUID,
    val userId: UUID,
) : AttractionEvent

data class CommuteBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteEvent

data class CommuteBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val commuteId: UUID,
    val userId: UUID,
    val seat: Seat,
) : CommuteEvent

data class TravelOfferBookedCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val userId: UUID,
    val seat: Seat,
) : TravelOfferEvent

data class TravelOfferBookingCanceledCompensatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override var correlationId: UUID?,
    override val travelOfferId: UUID,
    val accommodationId: UUID,
    val commuteId: UUID,
    val attractionId: UUID?,
    val userId: UUID,
    val seat: Seat,
) : TravelOfferEvent

fun Event.toCompensationEvent(): Event =
    when (this) {
        is AccommodationBookedEvent ->
            AccommodationBookedCompensatedEvent(
                correlationId = this.correlationId,
                accommodationId = this.accommodationId,
                userId = this.userId,
            )
        is AccommodationBookingCanceledEvent ->
            AccommodationBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                accommodationId = this.accommodationId,
                userId = this.userId,
            )
        is AttractionBookedEvent ->
            AttractionBookedCompensatedEvent(
                correlationId = this.correlationId,
                attractionId = this.attractionId,
                userId = this.userId,
            )
        is AttractionBookingCanceledEvent ->
            AttractionBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                attractionId = this.attractionId,
                userId = this.userId,
            )
        is CommuteBookedEvent ->
            CommuteBookedCompensatedEvent(
                correlationId = this.correlationId,
                commuteId = this.commuteId,
                userId = this.userId,
                seat = this.seat,
            )
        is CommuteBookingCanceledEvent ->
            CommuteBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                commuteId = this.commuteId,
                userId = this.userId,
                seat = this.seat,
            )
        is TravelOfferBookedEvent ->
            TravelOfferBookedCompensatedEvent(
                correlationId = this.correlationId,
                travelOfferId = this.travelOfferId,
                accommodationId = this.accommodationId,
                commuteId = this.commuteId,
                attractionId = this.attractionId,
                userId = this.userId,
                seat = this.seat,
            )
        is TravelOfferBookingCanceledEvent ->
            TravelOfferBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                travelOfferId = this.travelOfferId,
                accommodationId = this.accommodationId,
                commuteId = this.commuteId,
                attractionId = this.attractionId,
                userId = this.userId,
                seat = this.seat,
            )
        else -> throw IllegalArgumentException("Unsupported event type: ${this.javaClass.simpleName}")
    }
