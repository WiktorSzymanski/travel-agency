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
import pl.szymanski.wiktor.ta.subscribe

class AttractionEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
) {
    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AttractionExpiredEvent> {
                scope.launchCatching {
                    travelOfferService.expireTravelOfferByAttraction(it.event.attractionId, it.metadata.correlationId)
                }
            }
        }

    suspend fun attractionBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AttractionFullEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferUnavailableByAttraction(it.event.attractionId, it.metadata.correlationId)
                }
            }
        }

    suspend fun attractionBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AttractionAvailableEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferAvailableByAttraction(it.event.attractionId, it.metadata.correlationId)
                }
            }
        }
}
