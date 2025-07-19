package pl.szymanski.wiktor.ta.command.travelOffer

import pl.szymanski.wiktor.ta.command.Command
import pl.szymanski.wiktor.ta.domain.Seat
import java.util.UUID

sealed interface TravelOfferCommand : Command {
    val travelOfferId: UUID
}

data class BookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
    val seat: Seat,
) : TravelOfferCommand

data class CancelBookTravelOfferCommand(
    override val travelOfferId: UUID,
    override val correlationId: UUID,
    val userId: UUID,
    val seat: Seat,
) : TravelOfferCommand
