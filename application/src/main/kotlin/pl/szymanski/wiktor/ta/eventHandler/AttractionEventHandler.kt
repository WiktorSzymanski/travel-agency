package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.service.TravelOfferService

class AttractionEventHandler(
    private val travelOfferService: TravelOfferService,
) {
    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                scope.launchCatching {
                    travelOfferService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionFullEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferUnavailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionAvailableEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferAvailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }
}
