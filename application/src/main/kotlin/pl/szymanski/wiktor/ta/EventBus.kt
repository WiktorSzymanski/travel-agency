package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.Event
import java.time.LocalDateTime
import kotlin.reflect.KClass

interface EventBus {
    suspend fun publish(event: EventEnvelope<out Event>)
    suspend fun publishAtGivenTime(event: EventEnvelope<out Event>, date: LocalDateTime)
    suspend fun <T: Event> subscribe(eventType: KClass<T>, onEvent: suspend (EventEnvelope<T>) -> Unit)
}

suspend inline fun <reified T : Event> EventBus.subscribe(
    noinline onEvent: suspend (EventEnvelope<T>) -> Unit,
) = subscribe(T::class, onEvent)
