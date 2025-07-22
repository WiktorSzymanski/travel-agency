package pl.szymanski.wiktor.ta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.event.Event
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

object EventBus {
    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow()

    suspend fun publish(event: Any) {
        println("Publishing event: $event")
        _events.emit(event)
    }

    fun publish(
        event: Any,
        date: LocalDateTime,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        println("Event $event set to be published at: $date")
        scope.launch {
            Duration.between(LocalDateTime.now(zoneId), date).toMillis().let {
                if (it > 0) delay(it)
                publish(event)
            }
        }
    }

    suspend inline fun <reified T> subscribe(crossinline onEvent: suspend (T) -> Unit) {
        events.filterIsInstance<T>().collectLatest { event -> onEvent(event) }
    }

    suspend inline fun <reified T> subscribe(
        correlationId: UUID,
        crossinline onEvent: suspend (T) -> Unit,
    ) where T : Event {
        events
            .filterIsInstance<T>()
            .filter { event -> event.correlationId == correlationId }
            .collectLatest { event -> onEvent(event) }
    }
}
