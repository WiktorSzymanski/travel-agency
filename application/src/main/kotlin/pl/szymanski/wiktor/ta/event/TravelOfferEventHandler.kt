package pl.szymanski.wiktor.ta.event

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.BookingSaga
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent

class TravelOfferEventHandler(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    suspend fun setup() = coroutineScope {
        launch { travelOfferBookedEventHandler() }
        launch { travelOfferBookingCanceledEventHandler() }
    }

    suspend fun travelOfferBookedEventHandler() =
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

    suspend fun travelOfferBookingCanceledEventHandler() = coroutineScope {
        EventBus.subscribe<TravelOfferBookingCanceledEvent> {
            println("Travel offer booking canceled: ${it.travelOfferId}")
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
