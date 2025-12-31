package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferComponentId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferId
import pl.szymanski.wiktor.ta.dto.TravelOfferDto

interface TravelOfferQueryRepository {
    suspend fun save(entity: TravelOffer): TravelOffer?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(travelOfferId: TravelOfferId): TravelOffer

    suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer>

    suspend fun findTravelOfferDto(
        page: Int = 1,
        size: Int = 20,
        status: TravelOfferStatusEnum? = null,
        travelOfferId: TravelOfferId? = null,
    ): List<TravelOfferDto>

    suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int

    suspend fun findIdsByTravelOfferComponentId(id: TravelOfferComponentId): List<TravelOfferId>

    suspend fun findStatusesOfComponents(travelOfferId: TravelOfferId): Triple<CommuteStatusEnum, AccommodationStatusEnum, AttractionStatusEnum?>?
}
