package pl.szymanski.wiktor.ta.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    BOOKED,
    CANCEL_REQUESTED,
    CANCELED,
    FAILED,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
sealed interface Seat
data object AnySeat : Seat
data class PickedSeat(
    val row: String,
    val column: String
) : Seat

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
