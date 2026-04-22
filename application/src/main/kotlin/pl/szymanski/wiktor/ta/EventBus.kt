package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime

interface EventBus {
    suspend fun publish(eventEnvelope: EventEnvelope<out PublishableEvent>)
    suspend fun publishAt(eventEnvelope: EventEnvelope<out PublishableEvent>, date: LocalDateTime)
}
