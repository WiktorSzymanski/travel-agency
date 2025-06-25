package pl.szymanski.wiktor.ta.domain

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

data class Seat(
    val row: String,
    val column: String,
)

data class Rent(
    val from: LocalDateTime,
    val till: LocalDateTime,
)

enum class CommuteStatusEnum {
    SCHEDULED,
    CANCELLED,
    ARRIVED,
    DEPARTED,
}

enum class AccommodationStatusEnum {
    AVAILABLE,
    BOOKED,
    RENTING,
    EXPIRED,
}

enum class AttractionStatusEnum {
    SCHEDULED,
    CANCELLED,
    EXPIRED,
}

enum class OfferStatusEnum {
    AVAILABLE,
    BOOKED,
    EXPIRED,
    CANCELLED,
}
