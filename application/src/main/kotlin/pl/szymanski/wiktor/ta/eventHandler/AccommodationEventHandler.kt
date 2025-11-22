package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.service.TravelOfferService

class AccommodationEventHandler(
    private val travelOfferService: TravelOfferService,
) {
    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                scope.launchCatching {
                    travelOfferService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookedEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferUnavailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookingCanceledEvent> {
                scope.launchCatching {
                    travelOfferService.makeTravelOfferAvailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }
}
