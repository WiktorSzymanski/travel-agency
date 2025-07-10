package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationEnum
import java.time.LocalDateTime
import java.util.UUID

data class Attraction(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val date: LocalDateTime,
    val capacity: Int,
    val bookings: MutableList<Booking> = mutableListOf(),
    var status: AttractionStatusEnum = AttractionStatusEnum.SCHEDULED,
) {
    companion object {
        const val MINIMUM_REQUIRED_BOOKINGS_RATIO = 0.5
    }

    fun cancel() {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $_id cannot be cancelled when not in SCHEDULED status"
        }

        require(bookings.size < MINIMUM_REQUIRED_BOOKINGS_RATIO * capacity) {
            "Attraction $_id cannot be cancelled when more than half of seats are booked"
        }

        this.status = AttractionStatusEnum.CANCELLED

        // EVENT or something
    }

    fun expire() {
        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $_id cannot be cancelled when not in SCHEDULED status"
        }

        require(LocalDateTime.now().isAfter(date)) {
            "Attraction $_id cannot expire before its date"
        }

        this.status = AttractionStatusEnum.EXPIRED

        // EVENT or something
    }

    fun book(userId: UUID) {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Attraction $_id is not open for booking"
        }

        require(bookings.none { it.userId == userId }) {
            "User $userId already booked Attraction $_id"
        }

        require(bookings.size < capacity) {
            "Attraction $_id is fully booked"
        }

        bookings.add(Booking(userId, LocalDateTime.now()))

        // EVENT or something
    }

    fun cancelBooking(userId: UUID) {
        statusCheck()

        require(status == AttractionStatusEnum.SCHEDULED) {
            "Cannot cancel booking for Attraction $_id not in SCHEDULED status"
        }

        val removed = bookings.removeIf { it.userId == userId }

        require(removed) {
            "User $userId has no booking for Attraction $_id"
        }

        // EVENT or something
    }

    private fun statusCheck() {
        if (this.status == AttractionStatusEnum.SCHEDULED) {
            if (LocalDateTime.now().isAfter(date)) {
                if (bookings.size < MINIMUM_REQUIRED_BOOKINGS_RATIO * capacity) {
                    this.status = AttractionStatusEnum.CANCELLED
                } else {
                    this.status = AttractionStatusEnum.EXPIRED
                }
            }
        }
    }
}
