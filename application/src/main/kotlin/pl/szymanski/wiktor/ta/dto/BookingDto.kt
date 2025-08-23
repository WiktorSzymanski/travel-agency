package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.Booking

@Serializable
data class BookingDto(
    val id: String,
    val userId: String,
    val travelOfferId: String,
    val seat: Seat?,
    var status: String,
    var message: String? = null,
    val timestamp: String,
) {
    companion object {
        fun fromDomain(booking: Booking) =
            BookingDto(
                id = booking._id.toString(),
                userId = booking.userId.toString(),
                travelOfferId = booking.travelOfferId.toString(),
                seat = booking.seat,
                status = booking.status.name,
                message = booking.message,
                timestamp = booking.timestamp.toString(),
            )
    }
}
