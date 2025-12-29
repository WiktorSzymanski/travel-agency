package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.subscribe

class TravelOfferEventHandler(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOfferService: TravelOfferService,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {
    init {
        setupHandlers()
    }

    suspend fun travelOfferReleaseEventHandler() =
        eventBus.subscribe<TravelOfferReleaseEvent> {
            coroutineScope {
                launch {
                    CancelBookingSaga(
                        eventBus,
                        commandBus,
                        travelOfferService,
                        it.event,
                        it.metadata
                    ).execute()
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { travelOfferReleaseEventHandler() }
    )
}
