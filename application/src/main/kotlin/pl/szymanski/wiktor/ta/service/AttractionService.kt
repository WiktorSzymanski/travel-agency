package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository

class AttractionService(
    private val attractionRepository: AttractionQueryRepository
) {
    suspend fun checkAvailability(attractionId: AttractionId) {
        if (attractionId is AttractionId.Present) {
            val (attraction, _) = attractionRepository.findById(attractionId)
            attraction.checkAvailability()
        }
    }
}
