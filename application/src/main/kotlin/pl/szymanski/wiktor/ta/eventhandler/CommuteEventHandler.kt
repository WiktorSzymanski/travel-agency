package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.subscribe

class CommuteEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {
    init {
        setupHandlers()
    }

    suspend fun commuteExpiredEventHandler() =
        eventBus.subscribe<CommuteExpiredEvent> {
            coroutineScope {
                launch {
                    travelOfferService.expireTravelOfferByCommute(it.event.commuteId, it.metadata.correlationId)
                }
            }
        }

    suspend fun commuteBookedEventHandler() =
        eventBus.subscribe<CommuteFullEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferUnavailableByCommute(it.event.commuteId, it.metadata.correlationId)
                }
            }
        }

    suspend fun commuteBookingCanceledEventHandler() =
        eventBus.subscribe<CommuteAvailableEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferAvailableByCommute(it.event.commuteId, it.metadata.correlationId)
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { commuteExpiredEventHandler() },
        { commuteBookedEventHandler() },
        { commuteBookingCanceledEventHandler() },
    )
}
