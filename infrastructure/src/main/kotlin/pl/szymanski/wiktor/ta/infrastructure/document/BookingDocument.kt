package pl.szymanski.wiktor.ta.infrastructure.document

import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
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

@Document(collection = "bookings")
data class BookingDocument(
    @Id
    @Field("_id", targetType = FieldType.IMPLICIT)
    val id: UUID,
    val userId: String,
    val travelOffer: TravelOfferDocument,
    val seat: SeatDocument? = null,
    var status: String,
    var message: String? = null,
    val timestamp: String,
    @Version
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(booking: Booking, version: Long = 0L) =
            when (booking.id) {
                is BookingId.Present -> BookingDocument(
                    id = booking.id.value!!,
                    userId = booking.userId.toString(),
                    travelOffer = TravelOfferDocument.fromDomain(booking.travelOffer),
                    seat = SeatDocument.fromDomain(booking.seat),
                    status = booking.status.name,
                    message = booking.message,
                    timestamp = booking.timestamp.toString(),
                    version = version
                )

                is BookingId.Empty -> throw IllegalArgumentException("Booking ID cannot be empty when converting to document")
            }
    }

    fun toDomain(): Booking =
        Booking(
            id = BookingId.from(id) as BookingId.Present,
            userId = UUID.fromString(userId),
            travelOffer = travelOffer.toDomain(),
            seat = seat?.toDomain() ?: Seat.Any,
            status = BookingState.valueOf(status),
            message = message,
            timestamp = LocalDateTime.parse(timestamp)
        )
}

