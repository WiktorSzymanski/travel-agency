package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferService

class TravelOfferEventHandler(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOfferService: TravelOfferService,
) {
    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
//        scope.launch { travelOfferReservedEventHandler() }
        scope.launch { travelOfferReleaseEventHandler() }
    }

//    suspend fun travelOfferReservedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
//        coroutineScope {
//            EventBus.subscribe<TravelOfferReservedEvent> {
//                scope.launchCatching {
//                    BookingSaga(
//                        travelOfferCommandHandler,
//                        attractionCommandHandler,
//                        commuteCommandHandler,
//                        accommodationCommandHandler,
//                        travelOfferService,
//                        it,
//                    ).execute()
//                }
//            }
//        }

    suspend fun travelOfferReleaseEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<TravelOfferReleaseEvent> {
                scope.launchCatching {
                    CancelBookingSaga(
                        eventBus,
                        commandBus,
                        travelOfferService,
                        it,
                    ).execute()
                }
            }
        }
}
