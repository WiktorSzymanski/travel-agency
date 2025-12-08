package pl.szymanski.wiktor.ta

import pl.szymanski.wiktor.ta.domain.event.Event
import java.time.LocalDateTime

interface EventBus {
    suspend fun publish(event: Event)
    suspend fun publishAtGivenTime(event: Event, date: LocalDateTime)
    suspend fun <T> subscribe(onEvent: suspend (T) -> Unit)
}
