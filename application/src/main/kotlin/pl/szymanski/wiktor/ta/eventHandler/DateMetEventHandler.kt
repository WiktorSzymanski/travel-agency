package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.subscribe

class DateMetEventHandler(
    private val eventBus: EventBus,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { commuteDateMetEventHandler() }
        scope.launch { accommodationDateMetEventHandler() }
        scope.launch { attractionDateMetEventHandler() }
    }

    suspend fun commuteDateMetEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        eventBus.subscribe<CommuteDateMetEvent> {
            scope.launchCatching {
                commuteCommandHandler.handle(
                    ExpireCommuteCommand(
                        commuteId = it.event.commuteId,
                        correlationId = it.metadata.correlationId,
                    )
                )
            }
        }
    }

    suspend fun accommodationDateMetEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        eventBus.subscribe<AccommodationDateMetEvent> {
            scope.launchCatching {
                accommodationCommandHandler.handle(
                    ExpireAccommodationCommand(
                        accommodationId = it.event.accommodationId,
                        correlationId = it.metadata.correlationId,
                    ) as AccommodationCommand,
                )
            }
        }
    }

    suspend fun attractionDateMetEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        eventBus.subscribe<AttractionDateMetEvent> {
            scope.launchCatching {
                attractionCommandHandler.handle(
                    ExpireAttractionCommand(
                        attractionId = it.event.attractionId,
                        correlationId = it.metadata.correlationId,
                    ) as AttractionCommand,
                )
            }
        }
    }
}
