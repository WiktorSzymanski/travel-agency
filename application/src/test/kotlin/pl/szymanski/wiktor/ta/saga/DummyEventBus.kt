package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.Event
import java.time.LocalDateTime

class DummyEventBus : EventBus {
    val events = mutableListOf<Event>()

    override suspend fun publish(event: Event) {
        events.add(event)
    }

    override suspend fun publishAtGivenTime(
        event: Event,
        date: LocalDateTime
    ) {
        throw UnsupportedOperationException("Not needed for tests")
    }

    override suspend fun <T> subscribe(onEvent: suspend (T) -> Unit) {
        throw UnsupportedOperationException("Not needed for tests")
    }
}