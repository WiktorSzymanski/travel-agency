package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

interface TravelOfferRepository {
    suspend fun findById(travelOfferId: UUID): TravelOffer

    suspend fun save(travelOffer: TravelOffer): TravelOffer?

    suspend fun update(travelOffer: TravelOffer)

    suspend fun findByCommuteId(commuteId: UUID): List<TravelOffer>

    suspend fun findByAccommodationId(accommodationId: UUID): List<TravelOffer>

    suspend fun findByAttractionId(attractionId: UUID): List<TravelOffer>
}
