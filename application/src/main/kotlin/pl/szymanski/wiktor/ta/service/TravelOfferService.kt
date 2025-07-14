package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.BookingCoordinator
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferService(
    private val travelOfferRepository: TravelOfferRepository,
//    private val bookingCoordinator: BookingCoordinator,
) {
    suspend fun getTravelOffers(): List<TravelOffer> = travelOfferRepository.findAll()

//    suspend fun bookTravelOffer(
//        travelOfferId: UUID,
//        userId: UUID,
//        selectedSeat: Seat,
//    ): TravelOffer = bookingCoordinator.bookTravelOffer(travelOfferId, selectedSeat, userId)
//
//    suspend fun cancelTravelOfferBooking(
//        travelOfferId: UUID,
//        userId: UUID,
//    ): TravelOffer = bookingCoordinator.cancelTravelOfferBooking(travelOfferId, userId)
}
