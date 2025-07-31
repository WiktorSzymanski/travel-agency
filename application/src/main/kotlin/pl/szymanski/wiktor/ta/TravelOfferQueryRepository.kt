package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

interface TravelOfferQueryRepository {
    suspend fun findTravelOfferDto(
        page: Int = 1,
        size: Int = 20,
        status: TravelOfferStatusEnum? = null,
        travelOfferId: UUID? = null,
        userId: UUID? = null,
    ): List<TravelOfferDto>
}