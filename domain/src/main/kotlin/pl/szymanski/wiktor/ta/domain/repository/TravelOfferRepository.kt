package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

interface TravelOfferRepository {
    suspend fun findById(travelOfferId: UUID): TravelOffer?

    suspend fun save(travelOffer: TravelOffer): TravelOffer?

    suspend fun findAll(): List<TravelOffer>
}
