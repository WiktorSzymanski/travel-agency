package pl.szymanski.wiktor.ta

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import pl.szymanski.wiktor.ta.domain.event.Event
import java.util.UUID

object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow()

    suspend fun publish(event: Any) {
        println("Publishing event: $event")
        _events.emit(event)
    }

    suspend inline fun <reified T> subscribe(crossinline onEvent: suspend (T) -> Unit) {
        events.filterIsInstance<T>().collectLatest { event -> onEvent(event) }
    }

    suspend inline fun <reified T> subscribe(
        correlationId: UUID,
        crossinline onEvent: suspend (T) -> Unit
    ) where T : Event {
        events
            .filterIsInstance<T>()
            .filter { event -> event.correlationId == correlationId }
            .collectLatest { event -> onEvent(event) }
    }
}