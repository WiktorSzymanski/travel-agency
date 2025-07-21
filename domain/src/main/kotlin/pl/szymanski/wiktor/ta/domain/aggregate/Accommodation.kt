package pl.szymanski.wiktor.ta.domain.aggregate

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.Booking
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.Rent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
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
    companion object {
        fun create(
            name: String,
            location: LocationEnum,
            rent: Rent,
        ): Pair<Accommodation, AccommodationCreatedEvent> {
            val accommodation = Accommodation(
                name = name,
                location = location,
                rent = rent,
            )

            val event = AccommodationCreatedEvent(
                accommodationId = accommodation._id,
                name = name,
                location = location,
                rent = rent,
            )

            return accommodation to event
        }
    }

    fun expire(): AccommodationEvent {
        when (status) {
            AccommodationStatusEnum.AVAILABLE ->
                require(LocalDateTime.now().isAfter(rent.from)) {
                    "Accommodation $_id cannot be expired before its rent start"
                }
            else -> throw IllegalArgumentException(
                "Accommodation $_id cannot expire in status $status",
            )
        }

        this.status = AccommodationStatusEnum.EXPIRED

        return AccommodationExpiredEvent(
            accommodationId = _id,
        )
    }

    fun book(userId: UUID): AccommodationEvent {
        statusCheck()
        require(this.status == AccommodationStatusEnum.AVAILABLE) {
            "Accommodation $_id is not AVAILABLE"
        }

        this.status = AccommodationStatusEnum.BOOKED
        this.booking = Booking(userId, LocalDateTime.now())

        return AccommodationBookedEvent(
            accommodationId = _id,
            userId = userId,
        )
    }

    fun cancelBooking(userId: UUID): AccommodationEvent {
        statusCheck()
        require(this.status == AccommodationStatusEnum.BOOKED) {
            "Accommodation $_id is not BOOKED"
        }

        require(this.booking?.userId == userId) {
            "Accommodation $_id is not BOOKED by user $userId"
        }

        this.booking = null
        this.status = AccommodationStatusEnum.AVAILABLE

        return AccommodationBookingCanceledEvent(
            accommodationId = _id,
            userId = userId,
        )
    }

    private fun statusCheck() {
        if (!listOf(AccommodationStatusEnum.AVAILABLE, AccommodationStatusEnum.BOOKED).contains(this.status)) return
        if (LocalDateTime.now().isBefore(rent.from)) return

        this.status = AccommodationStatusEnum.EXPIRED
    }
}
