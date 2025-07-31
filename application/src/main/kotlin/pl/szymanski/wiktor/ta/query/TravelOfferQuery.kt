package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.UUID

class TravelOfferQuery(
    private val travelOfferRepository: TravelOfferQueryRepository,
    private val accommodationRepository: AccommodationQueryRepository
) {
    suspend fun getTravelOffers(page: Int, size: Int): List<TravelOfferDto> =
        travelOfferRepository.findTravelOfferDto(page, size)

    suspend fun getTravelOffersByStatus(status: TravelOfferStatusEnum, page: Int, size: Int): List<TravelOfferDto> =
        travelOfferRepository.findTravelOfferDto(page, size, status)

    suspend fun getTravelOfferById(travelOfferId: UUID): TravelOfferDto =
        travelOfferRepository.findTravelOfferDto(travelOfferId = travelOfferId)[0]

    suspend fun getTravelOfferByLocation(page: Int, size: Int, location: LocationEnum, status: TravelOfferStatusEnum): List<TravelOfferDto> =
        accommodationRepository.findTravelOfferByLocation(
            page = page,
            size = size,
            location = location,
            status = status
        )

    suspend fun getTravelOfferByUserId(page: Int, size: Int, userId: UUID): List<TravelOfferDto> =
        travelOfferRepository.findTravelOfferDto(
            page = page,
            size = size,
            userId = userId
        )
}
