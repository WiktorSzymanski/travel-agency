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
    val _id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var bookingId: UUID? = null,
    var status: TravelOfferStatusEnum = TravelOfferStatusEnum.AVAILABLE,
    val version: Int = 1,
) {
    companion object {
        fun create(
            name: String,
            commuteId: UUID,
            accommodationId: UUID,
            attractionId: UUID? = null,
        ): Pair<TravelOffer, TravelOfferCreatedEvent> {
            val travelOffer =
                TravelOffer(
                    _id = UUID.randomUUID(),
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            val event =
                TravelOfferCreatedEvent(
                    travelOfferId = travelOffer._id,
                    name = name,
                    commuteId = commuteId,
                    accommodationId = accommodationId,
                    attractionId = attractionId,
                )

            return travelOffer to event
        }
    }

    fun makeUnavailable(): TravelOfferEvent {
        require(this.status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer $_id cannot be made unavailable when in $status status"
        }

        this.status = TravelOfferStatusEnum.UNAVAILABLE

        return TravelOfferMadeUnavailableEvent(
            travelOfferId = _id,
        )
    }

    fun makeAvailable(): TravelOfferEvent {
        require(this.status == TravelOfferStatusEnum.UNAVAILABLE) {
            "TravelOffer $_id cannot be made available when in $status status"
        }

        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferMadeAvailableEvent(
            travelOfferId = _id,
        )
    }

    fun expire(): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer $_id cannot be expired when in $status status"
        }

        this.status = TravelOfferStatusEnum.EXPIRED

        return TravelOfferExpiredEvent(
            travelOfferId = _id,
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
            travelOfferId = _id,
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
            "TravelOffer $_id is not RESERVED by booking $bookingId"
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferBookedEvent(
            travelOfferId = _id,
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
            "Cannot cancel reservation for TravelOffer $_id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $_id is not RESERVED by user $bookingId"
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferReservationCanceledEvent(
            travelOfferId = _id,
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
            "Cannot cancel booking for TravelOffer $_id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $_id is not BOOKED for Booking $bookingId"
        }

        this.status = TravelOfferStatusEnum.RELEASING

        return TravelOfferReleaseEvent(
            travelOfferId = _id,
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
            "Cannot rebook TravelOffer $_id when in $status status"
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferRebookedEvent(
            travelOfferId = _id,
            bookingId = bookingId,
        )
    }

    fun cancelBooking(
        bookingId: UUID,
        seat: Seat?
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RELEASING) {
            "Cannot cancel Booking for TravelOffer $_id when in $status status"
        }

        require(this.bookingId == bookingId) {
            "TravelOffer $_id is not being released by Booking $bookingId"
        }

        this.bookingId = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferBookingCanceledEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            bookingId = bookingId,
            seat = seat
        )
    }
}
