package pl.szymanski.wiktor.ta.eventstore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class InMemoryEventStore : EventStore {
    private val events = mutableListOf<Pair<EventEnvelope<out PublishableEvent>, LocalDateTime?>>()
    private val subscribers = ConcurrentHashMap<Class<out PublishableEvent>, MutableList<suspend (EventEnvelope<out PublishableEvent>) -> Unit>>()

    override suspend fun append(event: EventEnvelope<out PublishableEvent>) {
        events.add(event to null)
        notifySubscribers(event)
    }

    override suspend fun appendAt(event: EventEnvelope<out PublishableEvent>, processAfter: LocalDateTime) {
        events.add(event to processAfter)
        // In a real implementation, a scheduler would check processAfter and dispatch when due
    }

    override suspend fun subscribe(
        eventType: Class<out PublishableEvent>,
        handler: suspend (EventEnvelope<out PublishableEvent>) -> Unit
    ) {
        subscribers.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    private fun notifySubscribers(event: EventEnvelope<out PublishableEvent>) {
        val handlers = subscribers[event.event::class.java] ?: return
        for (handler in handlers) {
            CoroutineScope(Dispatchers.Default).launch {
                handler(event)
            }
        }
    }
}

