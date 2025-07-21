package pl.szymanski.wiktor.ta.event

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.accommodation.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.commute.ExpireCommuteCommand

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
                )
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
                )
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
                )
            )
        }
    }
}