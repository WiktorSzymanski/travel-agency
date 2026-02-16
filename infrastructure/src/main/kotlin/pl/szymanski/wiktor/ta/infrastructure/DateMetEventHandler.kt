package pl.szymanski.wiktor.ta.infrastructure

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.repository.AttractionRepository
import pl.szymanski.wiktor.ta.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.DateMetEventHandleLogic
import pl.szymanski.wiktor.ta.subscribe

@Service
class DateMetEventHandler(
    commandBus: CommandBus,
    private val eventBus: EventBus,
    private val accommodationRepository: AccommodationRepository,
    private val commuteRepository: CommuteRepository,
    private val attractionRepository: AttractionRepository
) {
    private val handleLogic = DateMetEventHandleLogic(commandBus)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<CommuteDateMetEvent> {
                val (entity, events, metadata) = handleLogic.onCommuteDateMetEvent(it)
                commuteRepository.save(entity, events[0] as CommuteEvent, metadata)
            }
        }
        scope.launch {
            eventBus.subscribe<AccommodationDateMetEvent> {
                val (entity, events, metadata) = handleLogic.onAccommodationDateMetEvent(it)
                accommodationRepository.save(entity, events[0] as AccommodationEvent, metadata)
            }
        }
        scope.launch {
            eventBus.subscribe<AttractionDateMetEvent> {
                val (entity, events, metadata) = handleLogic.onAttractionDateMetEvent(it)
                attractionRepository.save(entity, events[0] as AttractionEvent, metadata)
            }
        }
    }
}
