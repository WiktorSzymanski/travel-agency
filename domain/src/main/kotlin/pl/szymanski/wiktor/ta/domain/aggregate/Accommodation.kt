package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import java.time.LocalDateTime
import java.util.UUID

data class Accommodation(
    val accommodationId: UUID,
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
                    "Accommodation $accommodationId cannot be expired before its from date"
                }
            else -> throw IllegalArgumentException(
                "Accommodation $accommodationId cannot expire in status $status",
            )
        }

        this.status = AccommodationStatusEnum.EXPIRED

        // EVENT or something
    }

    fun book(userId: UUID) {
        statusCheck()
        require(this.status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $accommodationId is not AVAILABLE"
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())

        // EVENT or something
    }

    fun cancelBooking(userId: UUID) {
        statusCheck()
        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $accommodationId is not BOOKED"
        }

        require(this.booking?.userId == userId) {
            "Accommodation $accommodationId is not BOOKED by user $userId"
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
