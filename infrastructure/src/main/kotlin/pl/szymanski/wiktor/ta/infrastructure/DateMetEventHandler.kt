package pl.szymanski.wiktor.ta.infrastructure

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<CommuteDateMetEvent> {
                onCommuteDateMetEvent(expireCommuteCommandHandler, it)
            }
        }
        scope.launch {
            eventBus.subscribe<AccommodationDateMetEvent> {
                onAccommodationDateMetEvent(expireAccommodationCommandHandler, it)
            }
        }
        scope.launch {
            eventBus.subscribe<AttractionDateMetEvent> {
                onAttractionDateMetEvent(expireAttractionCommandHandler, it)
            }
        }
    }
}
