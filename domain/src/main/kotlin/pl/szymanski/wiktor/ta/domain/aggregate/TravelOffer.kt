package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import java.time.LocalDateTime
import java.util.UUID

data class TravelOffer(
    val _id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var booking: Booking? = null,
    var status: TravelOfferStatusEnum = TravelOfferStatusEnum.AVAILABLE,
    val version: Int = 1
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
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.AVAILABLE) {
            "TravelOffer $_id is not open for reservation, current status is $status"
        }

        this.status = TravelOfferStatusEnum.RESERVED
        this.booking = Booking(userId, LocalDateTime.now())

        return TravelOfferReservedEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat,
        )
    }

    fun book(
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RESERVED) {
            "TravelOffer $_id is not open for booking, current status is $status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $_id is not RESERVED by user $userId"
        }

        this.status = TravelOfferStatusEnum.BOOKED

        return TravelOfferBookedEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat,
        )
    }

    fun cancelReservation(
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RESERVED) {
            "Cannot cancel reservation for TravelOffer $_id when in $status status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $_id is not RESERVED by user $userId"
        }

        this.booking = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferReservationCanceledEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat,
        )
    }

    fun cancelBooking(
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.BOOKED) {
            "Cannot cancel booking for TravelOffer $_id when in $status status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $_id is not BOOKED by user $userId"
        }

        this.status = TravelOfferStatusEnum.RELEASING

        return TravelOfferReleaseEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat,
        )
    }
    
    fun completeRelease(
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == TravelOfferStatusEnum.RELEASING) {
            "Cannot complete release for TravelOffer $_id when in $status status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $_id is not being released by user $userId"
        }

        this.booking = null
        this.status = TravelOfferStatusEnum.AVAILABLE

        return TravelOfferBookingCanceledEvent(
            travelOfferId = _id,
            accommodationId = accommodationId,
            commuteId = commuteId,
            attractionId = attractionId,
            userId = userId,
            seat = seat,
        )
    }
}
