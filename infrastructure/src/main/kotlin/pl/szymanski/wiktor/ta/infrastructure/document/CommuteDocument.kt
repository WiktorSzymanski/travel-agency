package pl.szymanski.wiktor.ta.infrastructure.document

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import pl.szymanski.wiktor.ta.domain.AnySeat
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.PickedSeat
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId

@Document(collection = "commutes")
data class CommuteDocument(
    @Id
    val id: CommuteId,
    val name: String,
    val departure: LocationAndTimeDocument,
    val arrival: LocationAndTimeDocument,
    val seats: List<String>,
    val bookings: Map<String, String>,
    val status: String,
    @Version
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(commute: Commute, version: Long = 0L) =
            CommuteDocument(
                id = commute.id,
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
            id = id,
            name = name,
            departure = departure.toDomain(),
            arrival = arrival.toDomain(),
            seats = seats.map { parseSeat(it) },
            bookings = bookings.mapKeys { BookingId.from(java.util.UUID.fromString(it.key)) }
                .mapValues { parseSeat(it.value) }
                .toMutableMap(),
            status = CommuteStatusEnum.valueOf(status)
        )

    private fun parseSeat(seatStr: String): Seat {
        return if (seatStr == "AnySeat") {
            AnySeat
        } else {
            val parts = seatStr.removePrefix("PickedSeat(").removeSuffix(")").split(", ")
            PickedSeat(
                row = parts[0].substringAfter("="),
                column = parts[1].substringAfter("="),
            )
        }
    }
}
