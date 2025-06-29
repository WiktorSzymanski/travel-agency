package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import java.util.UUID

interface BookingCoordinator {
    suspend fun bookTravelOffer(
        offerId: UUID,
        seat: Seat,
        userId: UUID,
    ): TravelOffer

    suspend fun cancelTravelOfferBooking(
        offerId: UUID,
        userId: UUID,
    ): TravelOffer
}
