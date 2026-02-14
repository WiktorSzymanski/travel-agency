package pl.szymanski.wiktor.ta.infrastructure.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking

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
) {
    companion object {
        fun fromDomain(booking: Booking) =
            BookingDto(
                id = booking.id.toString(),
                userId = booking.userId.toString(),
                travelOffer = TravelOfferDto.fromDomain(booking.travelOffer),
                seat = SeatDto.fromDomain(booking.seat),
                status = booking.status.name,
                message = booking.message,
                timestamp = booking.timestamp.toString(),
            )
    }
}
