package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

interface TravelOfferRepository {
    fun findById(travelOfferId: UUID): TravelOffer?

    fun save(travelOffer: TravelOffer)
}