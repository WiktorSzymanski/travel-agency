package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID

class BookingQuery(
    private val bookingRepository: BookingQueryRepository,
) {
    suspend fun getBookingById(bookingId: UUID) = bookingRepository.findById(bookingId)

    suspend fun getTravelOffersByUserId(
        page: Int,
        size: Int,
        userId: UUID,
    ) = bookingRepository.findTravelOfferDtoByUserId(page, size, userId)

    suspend fun getBookingsByUserId(
        page: Int,
        size: Int,
        userId: UUID,
    ) = bookingRepository.findByUserId(page, size, userId)
}
