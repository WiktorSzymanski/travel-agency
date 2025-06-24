package aggregate

import Booking
import OfferStatusEnum
import java.time.LocalDateTime
import java.util.UUID

data class TravelOffer(
    val travelOfferId: UUID,
    val name: String,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID?,
    var booking: Booking? = null,
    var status: OfferStatusEnum = OfferStatusEnum.AVAILABLE,
) {
    fun cancel() {
        status = OfferStatusEnum.CANCELLED
    }

    fun expire() {
        require(status == OfferStatusEnum.AVAILABLE) {
            "TravelOffer $travelOfferId cannot be cancelled when not in AVAILABLE status"
        }

        this.status = OfferStatusEnum.EXPIRED
        // EVENT: AttractionExpired
    }

    fun book(userId: UUID) {
        require(status == OfferStatusEnum.AVAILABLE) {
            "TravelOffer $travelOfferId is not open for booking"
        }

        this.status = OfferStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())
        // EVENT: BookingCreated
    }

    fun cancelBooking(userId: UUID) {
        require(status == OfferStatusEnum.BOOKED) {
            "Cannot cancel booking for TravelOffer $travelOfferId when not in BOOKED status"
        }

        require(this.booking?.userId == userId) {
            "TravelOffer $travelOfferId is not BOOKED by user $userId"
        }

        this.booking = null
        // EVENT: BookingCancelled
    }
}
