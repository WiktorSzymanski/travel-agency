package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.subscribe

class AccommodationEventHandler(
    private val eventBus: EventBus,
    private val travelOfferService: TravelOfferService,
) {
    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AccommodationExpiredEvent> {
                scope.launchCatching {
                    travelOfferService.expireTravelOfferByAccommodation(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }

    suspend fun accommodationBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AccommodationBookedEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferUnavailableByAccommodation(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }

    suspend fun accommodationBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<AccommodationBookingCanceledEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferAvailableByAccommodation(it.event.accommodationId, it.metadata.correlationId)
                }
            }
        }
}
