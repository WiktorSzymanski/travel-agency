package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

interface TravelOfferQueryRepository {
    suspend fun save(entity: TravelOffer): TravelOffer?

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(travelOfferId: UUID): TravelOffer

    suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer>

    suspend fun findTravelOfferDto(
        page: Int = 1,
        size: Int = 20,
        status: TravelOfferStatusEnum? = null,
        travelOfferId: UUID? = null,
    ): List<TravelOfferDto>
    
    suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int

    suspend fun findByCommuteId(commuteId: UUID): List<UUID>

    suspend fun findByAccommodationId(accommodationId: UUID): List<UUID>

    suspend fun findByAttractionId(attractionId: UUID): List<UUID>

    suspend fun findStatusesOfComponents(travelOfferId: UUID): Triple<CommuteStatusEnum, AccommodationStatusEnum, AttractionStatusEnum?>?

}