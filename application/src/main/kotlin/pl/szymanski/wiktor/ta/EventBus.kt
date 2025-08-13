package pl.szymanski.wiktor.ta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.EventRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

object EventBus {
    private val log = LoggerFactory.getLogger(EventBus::class.java)
    lateinit var repository: EventRepository

    fun init(repository: EventRepository) {
        this.repository = repository
    }

    suspend fun publish(event: Event) {
//        log.info("Publishing event: {}", event)
        repository.save(event)
    }

    fun publish(
        event: Event,
        date: LocalDateTime,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
//        log.info("Event {} set to be published at: {}", event, date)
        scope.launch {
            Duration.between(LocalDateTime.now(zoneId), date).toMillis().let {
                if (it > 0) delay(it)
                publish(event)
            }
        }
    }

    suspend inline fun <reified T> subscribe(crossinline onEvent: suspend (T) -> Unit) {
        repository.subscribe(T::class.java as Class<Event>) { event -> onEvent(event as T) }
    }
}
