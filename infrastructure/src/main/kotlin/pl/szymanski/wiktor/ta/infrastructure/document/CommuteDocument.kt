package pl.szymanski.wiktor.ta.infrastructure.document

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import java.util.UUID

@Serializable
data class CommuteDocument(
    val id: String,
    val name: String,
    val departure: LocationAndTimeDocument,
    val arrival: LocationAndTimeDocument,
    val seats: List<String>,
    val bookings: Map<String, String>,
    val status: String,
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(commute: Commute, version: Long = 0L) =
            CommuteDocument(
                id = commute.id.value.toString(),
                name = commute.name,
                departure = LocationAndTimeDocument.fromDomain(commute.departure),
                arrival = LocationAndTimeDocument.fromDomain(commute.arrival),
                seats = commute.seats.map { it.toString() },
                bookings = commute.bookings.mapKeys { it.key.value.toString() }.mapValues { it.value.toString() },
                status = commute.status.name,
                version = version
            )
    }

    fun toDomain(): Commute =
        Commute(
            id = CommuteId.from(UUID.fromString(id)),
            name = name,
            departure = departure.toDomain(),
            arrival = arrival.toDomain(),
            seats = seats.map { parseSeat(it) },
            bookings = bookings.mapKeys { BookingId.from(UUID.fromString(it.key)) }
                .mapValues { parseSeat(it.value) }
                .toMutableMap(),
            status = CommuteStatusEnum.valueOf(status)
        )

    private fun parseSeat(seatStr: String): Seat {
        return if (seatStr == "Any") {
            Seat.Any
        } else {
            val parts = seatStr.removePrefix("Picked(").removeSuffix(")").split(", ")
            Seat.Picked(
                row = parts[0].substringAfter("="),
                column = parts[1].substringAfter("="),
            )
        }
    }
}

