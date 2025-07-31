package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
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
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferEventHandler::class.java)
    }

    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { travelOfferBookedEventHandler() }
        scope.launch { travelOfferBookingCanceledEventHandler() }
        scope.launch { commuteExpiredEventHandler() }
        scope.launch { accommodationExpiredEventHandler() }
        scope.launch { attractionExpiredEventHandler() }
    }

    suspend fun travelOfferBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferBookedEvent> {
                scope.launch {
                    log.info("New travel offer booked: {}", it.travelOfferId)
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

    suspend fun travelOfferBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferBookingCanceledEvent> {
                scope.launch {
                log.info("Travel offer booking canceled: {}", it.travelOfferId)
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

    suspend fun commuteExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to commute expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to accommodation expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
                }

            }
        }

    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to attraction expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
                }
                }
            }
}
