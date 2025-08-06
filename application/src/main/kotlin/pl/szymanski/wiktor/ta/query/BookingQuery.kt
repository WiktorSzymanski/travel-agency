package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import java.util.UUID

class BookingQuery(
    private val bookingRepository: BookingRepository,
) {
    suspend fun getBookingById(bookingId: UUID) = bookingRepository.findById(bookingId)
}