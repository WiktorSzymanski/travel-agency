package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onAccommodationDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onAttractionDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCommuteDateMetEvent
import pl.szymanski.wiktor.ta.subscribe

@Service
class DateMetEventHandler(
    private val eventBus: EventBus,
    private val expireCommuteCommandHandler: ExpireCommuteCommandHandler,
    private val expireAccommodationCommandHandler: ExpireAccommodationCommandHandler,
    private val expireAttractionCommandHandler: ExpireAttractionCommandHandler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private val log = LoggerFactory.getLogger(DateMetEventHandler::class.java)
    }

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<CommuteDateMetEvent> {
                scope.launch {
                    runCatching { onCommuteDateMetEvent(expireCommuteCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling CommuteDateMetEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<AccommodationDateMetEvent> {
                scope.launch {
                    runCatching { onAccommodationDateMetEvent(expireAccommodationCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling AccommodationDateMetEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<AttractionDateMetEvent> {
                scope.launch {
                    runCatching { onAttractionDateMetEvent(expireAttractionCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling AttractionDateMetEvent", e) }
                }
            }
        }
    }
}
