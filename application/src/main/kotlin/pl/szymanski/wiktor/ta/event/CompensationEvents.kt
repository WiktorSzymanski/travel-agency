package pl.szymanski.wiktor.ta.event

import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent

fun AccommodationEvent.toCompensation(): AccommodationEvent =
    when (this) {
        is AccommodationBookedEvent ->
            AccommodationBookedCompensatedEvent(
                correlationId = this.correlationId,
                accommodationId = this.accommodationId,
                bookingId = this.bookingId,
            )
        is AccommodationBookingCanceledEvent ->
            AccommodationBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                accommodationId = this.accommodationId,
                bookingId = this.bookingId,
            )
        else -> throw IllegalArgumentException("Unsupported AccommodationEvent type: $this")
    }

fun AttractionEvent.toCompensation(): AttractionEvent =
    when (this) {
        is AttractionBookedEvent ->
            AttractionBookedCompensatedEvent(
                correlationId = this.correlationId,
                attractionId = this.attractionId,
                bookingId = this.bookingId,
            )
        is AttractionBookingCanceledEvent ->
            AttractionBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                attractionId = this.attractionId,
                bookingId = this.bookingId,
            )
        else -> throw IllegalArgumentException("Unsupported AttractionEvent type: $this")
    }

fun CommuteEvent.toCompensation(): CommuteEvent =
    when (this) {
        is CommuteBookedEvent ->
            CommuteBookedCompensatedEvent(
                correlationId = this.correlationId,
                commuteId = this.commuteId,
                bookingId = this.bookingId,
                seat = this.seat,
            )
        is CommuteBookingCanceledEvent ->
            CommuteBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                commuteId = this.commuteId,
                bookingId = this.bookingId,
                seat = this.seat,
            )
        else -> this
    }

fun TravelOfferEvent.toCompensation(): TravelOfferEvent =
    when (this) {
        is TravelOfferBookedEvent ->
            TravelOfferBookedCompensatedEvent(
                correlationId = this.correlationId,
                travelOfferId = this.travelOfferId,
                accommodationId = this.accommodationId,
                commuteId = this.commuteId,
                attractionId = this.attractionId,
                bookingId = this.bookingId,
                seat = this.seat,
            )
        is TravelOfferBookingCanceledEvent ->
            TravelOfferBookingCanceledCompensatedEvent(
                correlationId = this.correlationId,
                travelOfferId = this.travelOfferId,
                accommodationId = this.accommodationId,
                commuteId = this.commuteId,
                attractionId = this.attractionId,
                bookingId = this.bookingId,
                seat = this.seat,
            )
        is TravelOfferRebookedEvent ->
            this
        is TravelOfferReservationCanceledEvent -> this
        else -> this
    }
