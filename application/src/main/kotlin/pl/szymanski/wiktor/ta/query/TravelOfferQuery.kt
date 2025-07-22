package pl.szymanski.wiktor.ta.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.dto.TravelOfferDto

class TravelOfferQuery(
    private val travelOfferRepository: TravelOfferRepository,
    private val commuteRepository: CommuteRepository,
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
) {
    suspend fun getTravelOffers(): List<TravelOfferDto> = coroutineScope {
        travelOfferRepository.findAll()
            .map {
                val accommodation = async { accommodationRepository.findById(it.accommodationId) }
                val attraction =
                    if (it.attractionId != null) async { attractionRepository.findById(it.attractionId!!) } else null
                val commute = async { commuteRepository.findById(it.commuteId) }

                TravelOfferDto.fromDomain(it, commute.await(), accommodation.await(), attraction?.await())
            }
    }
}
