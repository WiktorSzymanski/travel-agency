package pl.szymanski.wiktor.ta.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

enum class LocationEnum {
    POZNAN,
    LONDON,
    PARIS,
    BERLIN,
}

data class LocationAndTime(
    val location: LocationEnum,
    val time: LocalDateTime,
)

data class Booking(
    val userId: UUID,
    val timestamp: LocalDateTime,
)

@Serializable
data class Seat(
    val row: String,
    val column: String,
) {
    override fun toString(): String = "$row|$column"

    companion object {
        fun fromString(seat: String): Seat {
            val (row, column) = seat.split("|")
            return Seat(row, column)
        }
    }
}

data class Rent(
    val from: LocalDateTime,
    val till: LocalDateTime,
)

enum class CommuteStatusEnum {
    SCHEDULED,
    FULL,
    EXPIRED,
}

enum class AccommodationStatusEnum {
    AVAILABLE,
    BOOKED,
    EXPIRED,
}

enum class AttractionStatusEnum {
    SCHEDULED,
    FULL,
    EXPIRED,
}

enum class TravelOfferStatusEnum {
    AVAILABLE,
    BOOKED,
    EXPIRED,
}
