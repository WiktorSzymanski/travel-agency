package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.OfferStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import java.time.LocalDateTime
import java.util.UUID

data class TravelOffer(
    val _id: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID? = null,
    var booking: Booking? = null,
    var status: OfferStatusEnum = OfferStatusEnum.AVAILABLE,
) {
    fun expire(): TravelOfferEvent {
        require(status == OfferStatusEnum.AVAILABLE) {
            "TravelOffer $_id cannot be cancelled when not in AVAILABLE status"
        }

        this.status = OfferStatusEnum.EXPIRED

        return TravelOfferExpiredEvent(
            travelOfferId = _id,
        )
    }

    fun book(
        userId: UUID,
        seat: Seat,
    ): TravelOfferEvent {
        require(status == OfferStatusEnum.AVAILABLE) {
            "TravelOffer $_id is not open for booking"
        }

        this.status = OfferStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())

        return TravelOfferBookedEvent(
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
        require(status == OfferStatusEnum.BOOKED) {
            "Cannot cancel booking for TravelOffer $_id when not in BOOKED status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $_id is not BOOKED by user $userId"
        }

        this.booking = null
        this.status = OfferStatusEnum.AVAILABLE

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
