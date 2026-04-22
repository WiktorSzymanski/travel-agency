package pl.szymanski.wiktor.ta.eventstore

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent

interface EventStore : EventBus {
    suspend fun subscribe(
        eventType: Class<out PublishableEvent>,
        handler: suspend (EventEnvelope<out PublishableEvent>) -> Unit
    )
}

