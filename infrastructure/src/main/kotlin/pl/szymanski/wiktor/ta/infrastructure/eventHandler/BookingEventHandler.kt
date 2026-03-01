package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCancelRequestedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onCreatedEvent
import pl.szymanski.wiktor.ta.saga.SagaService
import pl.szymanski.wiktor.ta.subscribe

@Service
class BookingEventHandler(
    private val eventBus: EventBus,
    private val sagaService: SagaService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private val log = LoggerFactory.getLogger(BookingEventHandler::class.java)
    }

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<BookingCreatedEvent> {
                scope.launch {
                    runCatching { onCreatedEvent(sagaService, it) }
                        .onFailure { e -> log.error("Error handling BookingCreatedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingCancelRequestedEvent> {
                scope.launch {
                    runCatching { onCancelRequestedEvent(sagaService, it) }
                        .onFailure { e -> log.error("Error handling BookingCancelRequestedEvent", e) }
                }
            }
        }
    }
}
