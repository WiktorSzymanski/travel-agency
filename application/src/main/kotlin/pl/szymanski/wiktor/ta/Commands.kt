package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface TravelOfferCommand {
    val travelOfferId: UUID
}

data class BookTravelOfferCommand(
    override val travelOfferId: UUID,
    val userId: UUID,
    val seat: Seat,
) : TravelOfferCommand

data class CancelBookTravelOfferCommand(
    override val travelOfferId: UUID,
    val userId: UUID,
) : TravelOfferCommand
