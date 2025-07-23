package pl.szymanski.wiktor.ta.query

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

class TravelOfferQuery(
    private val travelOfferRepository: TravelOfferRepository,
    private val commuteRepository: CommuteRepository,
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository,
) {
    suspend fun getTravelOffers(): List<TravelOfferDto> =
        travelOfferRepository.findAll()
            .map { it.toDto() }

    suspend fun getTravelOffersByStatus(status: TravelOfferStatusEnum): List<TravelOfferDto> =
        travelOfferRepository.findByStatus(status)
            .map { it.toDto() }

    suspend fun getTravelOfferById(travelOfferId: UUID): TravelOfferDto =
        travelOfferRepository.findById(travelOfferId).toDto()

    private suspend fun TravelOffer.toDto(): TravelOfferDto = coroutineScope {
        this@toDto.let {
            val accommodation = async { accommodationRepository.findById(this@toDto.accommodationId) }
            val attraction =
                if (this@toDto.attractionId != null) async { attractionRepository.findById(this@toDto.attractionId!!) } else null
            val commute = async { commuteRepository.findById(this@toDto.commuteId) }

            TravelOfferDto.fromDomain(this@toDto, commute.await(), accommodation.await(), attraction?.await())
        }
    }
}
