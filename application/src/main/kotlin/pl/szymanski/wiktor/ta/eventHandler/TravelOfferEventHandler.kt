package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.BookingSaga
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService

class TravelOfferEventHandler(
    private val travelOfferExpireService: TravelOfferExpireService,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { travelOfferBookedEventHandler() }
        scope.launch { travelOfferBookingCanceledEventHandler() }
        scope.launch { commuteExpiredEventHandler() }
        scope.launch { accommodationExpiredEventHandler() }
        scope.launch { attractionExpiredEventHandler() }
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

    suspend fun travelOfferBookingCanceledEventHandler() =
        coroutineScope {
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

    suspend fun commuteExpiredEventHandler() =
        coroutineScope {
            EventBus.subscribe<CommuteExpiredEvent> {
                println("TravelOffer expire due to commute expired event: $it")
                travelOfferExpireService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
            }
        }

    suspend fun accommodationExpiredEventHandler() =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                println("TravelOffer expire due to accommodation expired event: $it")
                travelOfferExpireService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
            }
        }

    suspend fun attractionExpiredEventHandler() =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                println("TravelOffer expire due to attraction expired event: $it")
                travelOfferExpireService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
            }
        }
}
