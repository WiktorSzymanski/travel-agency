package pl.szymanski.wiktor.ta.infrastructure.document

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class SeatDocument(
    val row: String,
    val column: String,
) {
    companion object {
        fun fromDomain(seat: Seat) = when (seat) {
            is Seat.Any -> throw IllegalArgumentException("Seat Any cannot be serialized to Document, it should be Picked by now")
            is Seat.Picked -> SeatDocument(seat.row, seat.column)
        }
    }

    fun toDomain(): Seat.Picked =
        Seat.Picked(row, column)
}

@Serializable
data class BookingDocument(
    val id: String,
    val userId: String,
    val travelOffer: TravelOfferDocument,
    val seat: SeatDocument? = null,
    var status: String,
    var message: String? = null,
    val timestamp: String,
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(booking: Booking, version: Long = 0L) =
            BookingDocument(
                id = booking.id.value.toString(),
                userId = booking.userId.toString(),
                travelOffer = TravelOfferDocument.fromDomain(booking.travelOffer),
                seat = SeatDocument.fromDomain(booking.seat),
                status = booking.status.name,
                message = booking.message,
                timestamp = booking.timestamp.toString(),
                version = version
            )
    }

    fun toDomain(): Booking =
        Booking(
            id = BookingId.from(UUID.fromString(id)) as BookingId.Present,
            userId = UUID.fromString(userId),
            travelOffer = travelOffer.toDomain(),
            seat = seat?.toDomain() ?: Seat.Any,
            status = BookingState.valueOf(status),
            message = message,
            timestamp = LocalDateTime.parse(timestamp)
        )
}

