package pl.szymanski.wiktor.ta.infrastructure

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.event.AccommodationDateMetEvent
import pl.szymanski.wiktor.ta.event.AttractionDateMetEvent
import pl.szymanski.wiktor.ta.event.CommuteDateMetEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.DateMetEventHandleLogic
import pl.szymanski.wiktor.ta.subscribe

class DateMetEventHandler(
    private val eventBus: EventBus,
    private val accommodationRepository: AccommodationRepository,
    private val commuteRepository: CommuteRepository,
    private val attractionRepository: AttractionRepository,
    private val logic: DateMetEventHandleLogic
) {
    suspend fun subscribe() {
        eventBus.subscribe<CommuteDateMetEvent> {
            val (entity, events) = logic.onCommuteDateMetEvent(it)
            commuteRepository.save(entity, events[0] as CommuteEvent)
        }
        eventBus.subscribe<AccommodationDateMetEvent> {
            val (entity, events) = logic.onAccommodationDateMetEvent(it)
            accommodationRepository.save(entity, events[0] as AccommodationEvent)
        }
        eventBus.subscribe<AttractionDateMetEvent> {
            val (entity, events) = logic.onAttractionDateMetEvent(it)
            attractionRepository.save(entity, events[0] as AttractionEvent)
        }
    }
}
