package pl.szymanski.wiktor.ta.service

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.BookingCoordinator
import java.util.UUID

class TravelOfferService(
    private val bookingCoordinator: BookingCoordinator,
) {
    suspend fun bookTravelOffer(
        travelOfferId: UUID,
        userId: UUID,
        selectedSeat: Seat,
    ): TravelOffer = bookingCoordinator.bookTravelOffer(travelOfferId, selectedSeat, userId)

    suspend fun cancelTravelOfferBooking(
        travelOfferId: UUID,
        userId: UUID,
    ): TravelOffer = bookingCoordinator.cancelTravelOfferBooking(travelOfferId, userId)
}
