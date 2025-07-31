package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.TravelOfferDto

interface AccommodationQueryRepository {
    suspend fun findTravelOfferByLocation(
        page: Int = 1,
        size: Int = 20,
        location: LocationEnum,
        status: TravelOfferStatusEnum? = null,
    ): List<TravelOfferDto>
}