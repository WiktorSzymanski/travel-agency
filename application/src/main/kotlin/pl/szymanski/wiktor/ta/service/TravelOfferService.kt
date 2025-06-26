package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


class TravelOfferService(
    private val travelOfferRepository: TravelOfferRepository,
    private val commuteRepository: CommuteRepository,
    private val accommodationRepository: AccommodationRepository,
    private val attractionRepository: AttractionRepository
) {
    suspend fun bookTravelOffer(
        travelOfferId: UUID,
        userId: UUID,
        selectedSeat: Seat
    ) = newSuspendedTransaction {
        val travelOffer = travelOfferRepository.findById(travelOfferId)
            ?: throw TravelOfferNotFoundException(travelOfferId)
        val commute = commuteRepository.findById(travelOffer.commuteId)
            ?: throw CommuteNotFoundException(travelOffer.commuteId)
        val accommodation = accommodationRepository.findById(travelOffer.accommodationId)
            ?: throw AccommodationNotFoundException(travelOffer.accommodationId)
        val attraction = travelOffer.attractionId?.let { attractionRepository.findById(it) }

        travelOffer.book(userId)
        commute.bookSeat(selectedSeat, userId)
        accommodation.book(userId)
        attraction?.book(userId)

        travelOfferRepository.save(travelOffer)
        commuteRepository.save(commute)
        accommodationRepository.save(accommodation)
        attraction?.let { attractionRepository.save(it) }
    }
}