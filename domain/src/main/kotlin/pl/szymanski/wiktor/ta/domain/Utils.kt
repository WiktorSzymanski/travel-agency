package pl.szymanski.wiktor.ta.domain

import java.time.LocalDateTime

enum class LocationEnum {
    POZNAN,
    LONDON,
    PARIS,
    BERLIN,
    ROME,
    AMSTERDAM,
    ZERMATT,
    VIENNA,
    BARCELONA,
    VENICE,
    ZURICH,
    BUDAPEST,
    MADRID,
    LYON,
    VALENCIA,
    MARSEILLE,
}

data class LocationAndTime(
    val location: LocationEnum,
    val time: LocalDateTime,
)

enum class BookingState {
    NEW,
    PROCESSING,
    BOOKED,
    CANCEL_REQUESTED,
    PROCESSING_CANCELLATION,
    CANCELED,
    FAILED,
}

sealed interface Seat {
    data object Any : Seat
    data class Picked(
        val row: String,
        val column: String
    ) : Seat
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
    RESERVED,
    BOOKED,
    UNAVAILABLE,
    EXPIRED,
    RELEASING,
}
