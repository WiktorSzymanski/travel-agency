package pl.szymanski.wiktor.ta.event

import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.BookingSaga
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent

class TravelOfferEventHandler(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    suspend fun setup() =
        coroutineScope {
            EventBus.subscribe<TravelOfferBookedEvent> {
                println("New travel offer booked: ${it.travelOfferId}")
                BookingSaga(
                    travelOfferCommandHandler,
                    attractionCommandHandler,
                    commuteCommandHandler,
                    accommodationCommandHandler,
                    it,
                ).execute()
            }
        }
}
