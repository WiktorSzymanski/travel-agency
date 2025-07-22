package pl.szymanski.wiktor.ta.dto

import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.Booking

@Serializable
data class BookingDto(
    val userId: String,
    val timestamp: String,
) {
    companion object {
        fun fromDomain(booking: Booking) =
            BookingDto(
                userId = booking.userId.toString(),
                timestamp = booking.timestamp.toString(),
            )
    }
}
