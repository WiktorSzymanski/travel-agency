package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.subscribe

class AccommodationEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {

    init {
        setupHandlers()
    }
    suspend fun accommodationExpiredEventHandler() =
        eventBus.subscribe<AccommodationExpiredEvent> {
            coroutineScope {
                launch {
                    travelOfferService.expireTravelOffer(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }

    suspend fun accommodationBookedEventHandler() =
        eventBus.subscribe<AccommodationBookedEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferUnavailable(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }

    suspend fun accommodationBookingCanceledEventHandler() =
        eventBus.subscribe<AccommodationBookingCanceledEvent> {
            coroutineScope {
                launch {
                    travelOfferService.makeTravelOfferAvailable(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { accommodationExpiredEventHandler() },
        { accommodationBookedEventHandler() },
        { accommodationBookingCanceledEventHandler() },
    )
}
