package pl.szymanski.wiktor.ta.eventbus

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.eventstore.EventStore
import java.time.LocalDateTime

class EventStoreBus(private val eventStore: EventStore) : EventBus {
    override suspend fun publish(event: EventEnvelope<out PublishableEvent>) {
        eventStore.append(event)
    }

    override suspend fun publishAtGivenTime(event: EventEnvelope<out PublishableEvent>, date: LocalDateTime) {
        eventStore.appendAt(event, date)
    }
}

