package pl.szymanski.wiktor.ta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.repository.EventRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateRevision
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

object EventBus {
    private val log = LoggerFactory.getLogger(EventBus::class.java)
    lateinit var repository: EventRepository

    fun init(repository: EventRepository) {
        this.repository = repository
    }

    suspend fun publish(events: List<Event>, revision: Long, etag: String?) {
        if (etag == null) {
            ignoreRevisionPublish(events[0])
        }
        else {
            if (events.size > 1) {
                repository.save(events, revision, etag)
            }
            else {
                repository.save(events[0], revision, etag)
            }
        }
    }

    suspend fun publish(event: Event, revision: Long, etag: String?) {
        if (etag == null)
            ignoreRevisionPublish(event)
        else
            repository.save(event, revision, etag)
    }

    suspend fun ignoreRevisionPublish(event: Event) {
        repository.noRevisionSave(event)
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
                ignoreRevisionPublish(event)
            }
        }
    }

    suspend inline fun <reified T> subscribe(crossinline onEvent: suspend (T) -> Unit) {
        repository.subscribe(T::class.java as Class<Event>) { event -> onEvent(event as T) }
    }
}
