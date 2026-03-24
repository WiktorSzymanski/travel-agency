package pl.szymanski.wiktor.ta.eventstore

import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent

interface EventStore {
    suspend fun append(event: EventEnvelope<out PublishableEvent>)
    suspend fun appendAt(event: EventEnvelope<out PublishableEvent>, processAfter: java.time.LocalDateTime)
    suspend fun subscribe(
        eventType: Class<out PublishableEvent>,
        handler: suspend (EventEnvelope<out PublishableEvent>) -> Unit
    )
}

