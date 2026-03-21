package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository

class AccommodationService(
    private val accommodationRepository: AccommodationQueryRepository
) {
    suspend fun checkAvailability(accommodationId: AccommodationId) {
        val (accommodation, _) = accommodationRepository.findById(accommodationId)
        accommodation.checkAvailability()
    }
}
