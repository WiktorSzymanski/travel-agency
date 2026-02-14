package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOfferComponentId
import pl.szymanski.wiktor.ta.dto.TravelOfferDto

interface TravelOfferQueryRepository {
    suspend fun save(entity: TravelOffer)

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer>

    suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int

    suspend fun findByTravelOfferComponentId(id: TravelOfferComponentId): List<TravelOffer>
}
