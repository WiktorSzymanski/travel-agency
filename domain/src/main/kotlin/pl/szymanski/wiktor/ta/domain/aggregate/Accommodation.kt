package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.time.LocalDateTime
import java.util.UUID

data class Accommodation(
    val _id: UUID = UUID.randomUUID(),
    val name: String,
    val location: LocationEnum,
    val rent: Rent,
    var booking: Booking? = null,
    var status: AccommodationStatusEnum = AccommodationStatusEnum.AVAILABLE,
) {
    fun expire() {
        when (status) {
            AccommodationStatusEnum.AVAILABLE ->
                require(LocalDateTime.now().isAfter(rent.from)) {
                    "Accommodation $_id cannot be expired before its from date"
                }
            else -> throw IllegalArgumentException(
                "Accommodation $_id cannot expire in status $status",
            )
        }

        this.status = AccommodationStatusEnum.EXPIRED

        // EVENT or something
    }

    fun book(userId: UUID) {
        statusCheck()
        require(this.status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $_id is not AVAILABLE"
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())

        // EVENT or something
    }

    fun cancelBooking(userId: UUID) {
        statusCheck()
        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $_id is not BOOKED"
        }

        require(this.booking?.userId == userId) {
            "Accommodation $_id is not BOOKED by user $userId"
        }

        this.booking = null

        // EVENT or something
    }

    private fun statusCheck() {
        if (this.status == AccommodationStatusEnum.AVAILABLE) {
            if (LocalDateTime.now().isAfter(rent.from)) {
                this.status = AccommodationStatusEnum.EXPIRED
            }
        }
    }
}
