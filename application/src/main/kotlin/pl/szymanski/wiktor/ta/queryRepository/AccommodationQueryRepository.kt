package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

interface AccommodationQueryRepository {
    suspend fun save(entity: Accommodation): Accommodation?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(accommodationId: UUID): Accommodation

    suspend fun findAllByStatus(status: AccommodationStatusEnum): List<Accommodation>

    suspend fun findTravelOfferByLocation(
        page: Int = 1,
        size: Int = 20,
        location: LocationEnum,
        status: TravelOfferStatusEnum? = null,
    ): List<TravelOfferDto>

    suspend fun countTravelOfferByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum? = null,
    ): Int
}
