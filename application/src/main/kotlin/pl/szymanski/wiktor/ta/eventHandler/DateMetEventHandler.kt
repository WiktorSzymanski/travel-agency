package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireCommuteCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent

class DateMetEventHandler(
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    suspend fun setup() = coroutineScope {
        launch { commuteDateMetEventHandler() }
        launch { accommodationDateMetEventHandler() }
        launch { attractionDateMetEventHandler() }
    }

    suspend fun commuteDateMetEventHandler() {
        EventBus.subscribe<CommuteDateMetEvent> {
            println("Commute date met event: $it")
            commuteCommandHandler.handle(
                ExpireCommuteCommand(
                    commuteId = it.commuteId,
                    correlationId = it.correlationId,
                ) as CommuteCommand
            )

        }
    }

    suspend fun accommodationDateMetEventHandler() {
        EventBus.subscribe<AccommodationDateMetEvent> {
            println("Accommodation date met event: $it")
            accommodationCommandHandler.handle(
                ExpireAccommodationCommand(
                    accommodationId = it.accommodationId,
                    correlationId = it.correlationId,
                ) as AccommodationCommand
            )
        }
    }

    suspend fun attractionDateMetEventHandler() {
        EventBus.subscribe<AttractionDateMetEvent> {
            println("Attraction date met event: $it")
            attractionCommandHandler.handle(
                ExpireAttractionCommand(
                    attractionId = it.attractionId,
                    correlationId = it.correlationId,
                ) as AttractionCommand
            )
        }
    }
}