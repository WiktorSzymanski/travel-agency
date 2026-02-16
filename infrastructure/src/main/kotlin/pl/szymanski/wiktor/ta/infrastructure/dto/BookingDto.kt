package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class SeatDto(
    val row: String,
    val column: String,
) {
    companion object {
        fun fromDomain(seat: Seat) = when (seat) {
            is Seat.Any -> throw IllegalArgumentException("Seat Any cannot be serialized to DTO, it should be Picked by now")
            is Seat.Picked -> SeatDto(seat.row, seat.column)
        }
    }

    fun toDomain(): Seat.Picked =
        Seat.Picked(row, column)
}

@Serializable
data class BookingDto(
    val id: String,
    val userId: String,
    val travelOffer: TravelOfferDto,
    val seat: SeatDto? = null,
    var status: String,
    var message: String? = null,
    val timestamp: String,
    val version: Long = 0L
) {
    companion object {
        fun fromDomain(booking: Booking, version: Long = 0L) =
            BookingDto(
                id = booking.id.value.toString(),
                userId = booking.userId.toString(),
                travelOffer = TravelOfferDto.fromDomain(booking.travelOffer),
                seat = SeatDto.fromDomain(booking.seat),
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
