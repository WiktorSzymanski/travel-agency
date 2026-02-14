package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID

class BookingQuery(
    private val bookingRepository: BookingQueryRepository,
) {
    suspend fun getBookingById(id: BookingId) = bookingRepository.findById(id)
}
