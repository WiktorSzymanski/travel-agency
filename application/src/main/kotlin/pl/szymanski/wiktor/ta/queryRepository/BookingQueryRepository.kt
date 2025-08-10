package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

interface BookingQueryRepository {
    suspend fun findTravelOfferDtoByUserId(
        page: Int = 1,
        size: Int = 20,
        userId: UUID,
    ): List<TravelOfferDto>

    suspend fun findById(bookingId: UUID): Booking

    suspend fun findByUserId(
        page: Int = 1,
        size: Int = 20,
        userId: UUID
    ): List<Booking>
}
