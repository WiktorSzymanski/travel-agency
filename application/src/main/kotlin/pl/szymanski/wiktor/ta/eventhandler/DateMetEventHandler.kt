package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.subscribe

class DateMetEventHandler(
    private val eventBus: EventBus,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {

    init {
        setupHandlers()
    }
    suspend fun commuteDateMetEventHandler() =
        eventBus.subscribe<CommuteDateMetEvent> {
            coroutineScope {
                launch {
                    commuteCommandHandler.handle(
                        ExpireCommuteCommand(
                            commuteId = it.event.commuteId,
                            correlationId = it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun accommodationDateMetEventHandler() =
        eventBus.subscribe<AccommodationDateMetEvent> {
            coroutineScope {
                launch {
                    accommodationCommandHandler.handle(
                        ExpireAccommodationCommand(
                            accommodationId = it.event.accommodationId,
                            correlationId = it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun attractionDateMetEventHandler() =
        eventBus.subscribe<AttractionDateMetEvent> {
            coroutineScope {
                launch {
                    attractionCommandHandler.handle(
                        ExpireAttractionCommand(
                            attractionId = it.event.attractionId,
                            correlationId = it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { commuteDateMetEventHandler() },
        { accommodationDateMetEventHandler() },
        { attractionDateMetEventHandler() },
    )
}
