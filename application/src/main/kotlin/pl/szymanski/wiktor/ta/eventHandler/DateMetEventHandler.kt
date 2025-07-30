package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
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
    companion object {
        private val log = LoggerFactory.getLogger(DateMetEventHandler::class.java)
    }

    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { commuteDateMetEventHandler() }
        scope.launch { accommodationDateMetEventHandler() }
        scope.launch { attractionDateMetEventHandler() }
    }

    suspend fun commuteDateMetEventHandler() {
        EventBus.subscribe<CommuteDateMetEvent> {
            log.info("Commute date met event: {}", it)
            try {
                commuteCommandHandler.handle(
                    ExpireCommuteCommand(
                        commuteId = it.commuteId,
                        correlationId = it.correlationId,
                    ) as CommuteCommand,
                )
            } catch (e: IllegalArgumentException) {
                log.error("ERROR HANDLE: {}", e.message)
            }
        }
    }

    suspend fun accommodationDateMetEventHandler() {
        EventBus.subscribe<AccommodationDateMetEvent> {
            log.info("Accommodation date met event: {}", it)
            try {
                accommodationCommandHandler.handle(
                    ExpireAccommodationCommand(
                        accommodationId = it.accommodationId,
                        correlationId = it.correlationId,
                    ) as AccommodationCommand,
                )
            } catch (e: IllegalArgumentException) {
                log.error("ERROR HANDLE: {}", e.message)
            }
        }
    }

    suspend fun attractionDateMetEventHandler() {
        EventBus.subscribe<AttractionDateMetEvent> {
            log.info("Attraction date met event: {}", it)
            try {
                attractionCommandHandler.handle(
                    ExpireAttractionCommand(
                        attractionId = it.attractionId,
                        correlationId = it.correlationId,
                    ) as AttractionCommand,
                )
            } catch (e: IllegalArgumentException) {
                log.error("ERROR HANDLE: {}", e.message)
            }
        }
    }
}
