package aggregate

import AccommodationStatusEnum
import Booking
import LocationEnum
import Rent
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
            AccommodationStatusEnum.RENTING ->
                require(LocalDateTime.now().isAfter(rent.till)) {
                    "Accommodation $accommodationId cannot expire while RENTING"
                }
            else -> throw IllegalArgumentException(
                "Accommodation $accommodationId cannot expire in status $status",
            )
        }

        this.status = AccommodationStatusEnum.EXPIRED

        // EVENT or something
    }

    fun book(userId: UUID) {
        require(this.status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $accommodationId is not AVAILABLE"
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())

        // EVENT or something
    }

    fun cancelBooking(userId: UUID) {
        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $accommodationId is not BOOKED"
        }

        require(this.booking?.userId == userId) {
            "Accommodation $accommodationId is not BOOKED by user $userId"
        }

        this.booking = null

        // EVENT or something
    }

    fun renting() {
        require(LocalDateTime.now().isAfter(rent.from)) {
            "Accommodation $accommodationId cannot be in RENTING before ${rent.from}"
        }

        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $accommodationId is not BOOKED so it cannot be in RENTING"
        }

        this.status = AccommodationStatusEnum.RENTING

        // EVENT or something
    }
}
