package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository

class TravelOfferQuery(
    private val travelOfferRepository: TravelOfferRepository,
) {
    suspend fun getTravelOffers(): List<TravelOffer> = travelOfferRepository.findAll()
}
