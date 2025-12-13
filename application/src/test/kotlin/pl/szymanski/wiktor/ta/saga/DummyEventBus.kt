package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.Event
import java.time.LocalDateTime
import kotlin.reflect.KClass

class DummyEventBus : EventBus {
    val events = mutableListOf<EventEnvelope<out Event>>()

    override suspend fun publish(event: EventEnvelope<out Event>) {
        events.add(event)
    }

    override suspend fun publishAtGivenTime(
        event: EventEnvelope<out Event>,
        date: LocalDateTime
    ) {
        throw UnsupportedOperationException("Not needed for tests")
    }

    override suspend fun <T : Event> subscribe(
        eventType: KClass<T>,
        onEvent: suspend (EventEnvelope<T>) -> Unit
    ) {
        throw UnsupportedOperationException("Not needed for tests")
    }
}