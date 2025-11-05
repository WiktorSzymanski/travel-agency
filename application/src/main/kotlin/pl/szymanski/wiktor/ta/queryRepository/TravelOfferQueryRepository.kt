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

    suspend fun findById(travelOfferId: UUID): TravelOffer

    suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer>

    suspend fun update(entity: TravelOfferUpdate, event: Event)

    suspend fun update(entity: TravelOfferUpdateRevision, event: Event)

    suspend fun update(entity: TravelOfferUpdateStatus, event: Event)

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

data class TravelOfferUpdateRevision(
    val id: UUID,
    val lastRevision: Int,
    val event: Event
)

data class TravelOfferUpdate(
    val id: UUID,
    val status: TravelOfferStatusEnum? = null,
    val bookingId: UUID? = null,
    val lastRevision: Int
)

data class TravelOfferUpdateStatus(
    val id: UUID,
    val status: TravelOfferStatusEnum? = null,
    val lastRevision: Int
)
