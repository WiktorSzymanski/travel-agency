package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.subscribe

class AttractionEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {
    init {
        setupHandlers()
    }

    suspend fun attractionExpiredEventHandler() =
        eventBus.subscribe<AttractionExpiredEvent> {
            coroutineScope {
                launch {
                    travelOfferService.expireTravelOfferByAttraction(it.event.attractionId, it.metadata.correlationId)
                }
            }
        }

    suspend fun attractionBookedEventHandler() =
        eventBus.subscribe<AttractionFullEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferUnavailableByAttraction(
                        it.event.attractionId,
                        it.metadata.correlationId
                    )
                }
            }
        }

    suspend fun attractionBookingCanceledEventHandler() =
        eventBus.subscribe<AttractionAvailableEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferAvailableByAttraction(it.event.attractionId, it.metadata.correlationId)
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { attractionExpiredEventHandler() },
        { attractionBookedEventHandler() },
        { attractionBookingCanceledEventHandler() },
    )
}
