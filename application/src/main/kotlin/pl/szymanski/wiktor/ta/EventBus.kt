package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import java.time.LocalDateTime
import kotlin.reflect.KClass

interface EventBus {
    suspend fun publish(event: EventEnvelope<out PublishableEvent>)
    suspend fun publishAtGivenTime(event: EventEnvelope<out PublishableEvent>, date: LocalDateTime)
}
