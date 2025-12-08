package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.service.TravelOfferService

class CommuteEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
) {
    suspend fun commuteExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<CommuteExpiredEvent> {
                scope.launchCatching {
                    travelOfferService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<CommuteFullEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferUnavailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<CommuteAvailableEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferAvailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }
}
