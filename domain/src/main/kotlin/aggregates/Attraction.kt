package aggregates

import AttractionStatusEnum
import Booking
import LocationEnum
import java.time.LocalDateTime
import java.util.UUID

data class Attraction(
    val attractionId: UUID,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<Booking> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED
) {
    fun cancel() {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $attractionId cannot be cancelled when not in SCHEDULED status"
        }

        require(bookings.size < 0.5 * capacity) {
            "Attraction $attractionId cannot be cancelled when more than half of seats are booked"
        }

        this.status = AttractionStatusEnum.CANCELLED
        // EVENT: AttractionCancelled
    }

    fun expire() {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $attractionId cannot be cancelled when not in SCHEDULED status"
        }

        require(LocalDateTime.now().isAfter(date)) {
            "Attraction $attractionId cannot expire before its date"
        }

        this.status = AttractionStatusEnum.EXPIRED
        // EVENT: AttractionExpired
    }

    fun book(userId: UUID) {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $attractionId is not open for booking"
        }

        require(bookings.none { it.userId == userId }) {
            "User $userId already booked Attraction $attractionId"
        }

        require(bookings.size < capacity) {
            "Attraction $attractionId is fully booked"
        }

        bookings.add(Booking(userId, LocalDateTime.now()))
        // EVENT: BookingCreated
    }

    fun cancelBooking(userId: UUID) {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Cannot cancel booking for Attraction $attractionId not in SCHEDULED status"
        }

        val removed = bookings.removeIf { it.userId == userId }

        require(removed) {
            "User $userId has no booking for Attraction $attractionId"
        }

        // EVENT: BookingCancelled
    }
}
