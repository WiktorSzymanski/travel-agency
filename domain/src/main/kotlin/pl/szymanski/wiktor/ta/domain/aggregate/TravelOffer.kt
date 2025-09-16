package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.util.UUID

data class TravelOffer(
    val id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var bookingId: UUID? = null,
    var status: TravelOfferStatusEnum = TravelOfferStatusEnum.AVAILABLE,
    val lastRevision: Int = -1,
    val lastEtag : String? = null
) {
    fun apply(event: TravelOfferEvent, revision: Int, etag: String): TravelOffer {
        return when (event) {
            is TravelOfferCreatedEvent -> this.copy(
                id = event.travelOfferId,
                name = event.name,
                commuteId = event.commuteId,
                accommodationId = event.accommodationId,
                attractionId = event.attractionId,
                status = TravelOfferStatusEnum.AVAILABLE,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferMadeUnavailableEvent -> this.copy(
                status = TravelOfferStatusEnum.UNAVAILABLE,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferMadeAvailableEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferExpiredEvent -> this.copy(
                status = TravelOfferStatusEnum.EXPIRED,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferReservedEvent -> this.copy(
                status = TravelOfferStatusEnum.RESERVED,
                bookingId = event.bookingId,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferBookedEvent -> this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                bookingId = event.bookingId,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferReservationCanceledEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferReleaseEvent -> this.copy(
                status = TravelOfferStatusEnum.RELEASING,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferRebookedEvent -> this.copy(
                status = TravelOfferStatusEnum.BOOKED,
                lastRevision = revision,
                lastEtag = etag
            )
            is TravelOfferBookingCanceledEvent -> this.copy(
                status = TravelOfferStatusEnum.AVAILABLE,
                bookingId = null,
                lastRevision = revision,
                lastEtag = etag
            )
            else -> this.copy(lastRevision = revision,
                lastEtag = etag)
        }
    }
    companion object {
        fun create(
            name: String,
            commuteId: UUID,
            accommodationId: UUID,
            attractionId: UUID? = null,
        ): Pair<TravelOffer, TravelOfferCreatedEvent> {
            val travelOffer =
                TravelOffer(
                    id = UUID.randomUUID(),
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            val event =
                TravelOfferCreatedEvent(
                    travelOfferId = travelOffer.id,
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            return travelOffer to event
        }
        
        fun fromEvents(events: List<Triple<TravelOfferEvent, Int, String>>): TravelOffer? {
            if (events.isEmpty()) return null

            val (createdEvent, _) = events.first()

            require(createdEvent is TravelOfferCreatedEvent) { "First event must be TravelOfferCreatedEvent" }

            var travelOffer = TravelOffer(
                id = createdEvent.travelOfferId,
                name = createdEvent.name,
                commuteId = createdEvent.commuteId,
                accommodationId = createdEvent.accommodationId,
                attractionId = createdEvent.attractionId,
            )
            
            // Apply all events in order to reconstruct the current state
            for ((event, revision, etag) in events) {
                travelOffer = travelOffer.apply(event, revision, etag)
            }
            
            return travelOffer
        }
    }

    fun makeUnavailable(): TravelOfferEvent {
        require(this.status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer $id cannot be made unavailable when in $status status"
        }

        this.status = TravelOfferStatusEnum.UNAVAILABLE

        return TravelOfferMadeUnavailableEvent(
            travelOfferId = id,
        )
    }

    fun makeAvailable(): TravelOfferEvent {
        require(this.status == TravelOfferStatusEnum.UNAVAILABLE) {
            "TravelOffer $id cannot be made available when in $status status"
        }

        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferMadeAvailableEvent(
            travelOfferId = id,
        )
    }

    fun expire(): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer $id cannot be expired when in $status status"
        }

        this.status = TravelOfferStatusEnum.EXPIRED

        return TravelOfferExpiredEvent(
            travelOfferId = id,
            commuteId = commuteId,
            accommodationId = accommodationId,
            attractionId = attractionId,
        )
    }

    fun reserve(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer is not open for reservation, current status is $status"
        }

        this.status = TravelOfferStatusEnum.RESERVED
        this.bookingId = bookingId

        return TravelOfferReservedEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun book(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RESERVED) {
            "TravelOffer can not be booked if not RESERVED prior, current status is $status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $id is not RESERVED by booking $bookingId"
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferBookedEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    fun cancelReservation(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RESERVED) {
            "Cannot cancel reservation for TravelOffer $id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $id is not RESERVED by user $bookingId"
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferReservationCanceledEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }

    fun releaseBooking(
        bookingId: UUID,
        seat: Seat?,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.BOOKED) {
            "Cannot cancel booking for TravelOffer $id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $id is not BOOKED for Booking $bookingId"
        }

        this.status = TravelOfferStatusEnum.RELEASING

        return TravelOfferReleaseEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }

    fun rebook(
        bookingId: UUID,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RELEASING) {
            "Cannot rebook TravelOffer $id when in $status status"
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferRebookedEvent(
            travelOfferId = id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(
        bookingId: UUID,
        seat: Seat?
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RELEASING) {
            "Cannot cancel Booking for TravelOffer $id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $id is not being released by Booking $bookingId"
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferBookingCanceledEvent(
            travelOfferId = id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }
}