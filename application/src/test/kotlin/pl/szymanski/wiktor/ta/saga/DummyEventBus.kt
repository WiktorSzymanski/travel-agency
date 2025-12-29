package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import kotlin.reflect.KClass

class DummyEventBus : EventBus {
    val emittedEvents = mutableListOf<EventEnvelope<out PublishableEvent>>()
    private val _events = MutableSharedFlow<EventEnvelope<out PublishableEvent>>()
    val events = _events.asSharedFlow()

    override suspend fun publish(event: EventEnvelope<out PublishableEvent>) {
        _events.emit(event)
        emittedEvents.add(event)
    }

    override suspend fun publishAtGivenTime(
        event: EventEnvelope<out PublishableEvent>,
        date: LocalDateTime
    ) {
        throw UnsupportedOperationException("Not needed for tests")
    }

    override suspend fun <T : PublishableEvent> subscribe(
        eventType: KClass<T>,
        onEvent: suspend (EventEnvelope<T>) -> Unit
    ) {
        events.filter { it.eventType == eventType.java.simpleName }.collectLatest { event -> onEvent(event as EventEnvelope<T>) }
    }
}