package pl.szymanski.wiktor.ta.query

import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import java.util.UUID

class TravelOfferQuery(
    private val travelOfferRepository: TravelOfferQueryRepository,
    private val accommodationRepository: AccommodationQueryRepository,
) {
    suspend fun getTravelOffers(
        page: Int,
        size: Int,
    ): List<TravelOfferDto> = travelOfferRepository.findTravelOfferDto(page, size)

    suspend fun getTravelOffersByStatus(
        status: TravelOfferStatusEnum,
        page: Int,
        size: Int,
    ): List<TravelOfferDto> = travelOfferRepository.findTravelOfferDto(page, size, status)

    suspend fun getTravelOfferById(travelOfferId: UUID): TravelOfferDto =
        travelOfferRepository.findTravelOfferDto(travelOfferId = travelOfferId).firstOrNull() ?: throw NoSuchElementException()

    suspend fun getTravelOfferByLocation(
        page: Int,
        size: Int,
        location: LocationEnum,
        status: TravelOfferStatusEnum,
    ): List<TravelOfferDto> =
        accommodationRepository.findTravelOfferByLocation(
            page = page,
            size = size,
            location = location,
            status = status,
        )

    suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int {
        return travelOfferRepository.countTravelOffersByStatus(status)
    }

    suspend fun countTravelOffersByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum,
    ): Int {
        return accommodationRepository.countTravelOfferByLocation(location, status)
    }
}
