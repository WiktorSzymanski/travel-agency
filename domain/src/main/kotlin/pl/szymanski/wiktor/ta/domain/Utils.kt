package pl.szymanski.wiktor.ta.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
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

@Serializable
data class LocationAndTime(
    val location: LocationEnum,
    @Serializable(with = LocalDateTimeSerializer::class)
    val time: LocalDateTime,
)

@Serializable
enum class BookingState {
    NEW,
    PROCESSING,
    BOOKED,
    CANCEL_REQUESTED,
    PROCESSING_CANCELLATION,
    CANCELED,
    FAILED,
}

@Serializable
sealed interface Seat {
    @Serializable
    data object Any : Seat
    @Serializable
    data class Picked(
        val row: String,
        val column: String
    ) : Seat
}

@Serializable
data class Rent(
    @Serializable(with = LocalDateTimeSerializer::class)
    val from: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val till: LocalDateTime,
)

@Serializable
enum class CommuteStatusEnum {
    SCHEDULED,
    FULL,
    EXPIRED,
}

@Serializable
enum class AccommodationStatusEnum {
    AVAILABLE,
    BOOKED,
    EXPIRED,
}

@Serializable
enum class AttractionStatusEnum {
    SCHEDULED,
    FULL,
    EXPIRED,
}

@Serializable
enum class TravelOfferStatusEnum {
    AVAILABLE,
    RESERVED,
    BOOKED,
    UNAVAILABLE,
    EXPIRED,
    RELEASING,
}
