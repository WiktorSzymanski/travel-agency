package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<BookingCreatedEvent> {
                onCreatedEvent(sagaService, it)
            }
        }
        scope.launch {
            eventBus.subscribe<BookingCancelRequestedEvent> {
                onCancelRequestedEvent(sagaService, it)
            }
        }
    }
}
