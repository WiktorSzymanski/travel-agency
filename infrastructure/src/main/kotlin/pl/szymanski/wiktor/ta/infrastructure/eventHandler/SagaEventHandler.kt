package pl.szymanski.wiktor.ta.infrastructure.eventHandler

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingSagaFailedEvent
import pl.szymanski.wiktor.ta.eventHandlerLogic.onBookingSagaStartedEvent
import pl.szymanski.wiktor.ta.subscribe

@Service
class SagaEventHandler(
    private val eventBus: EventBus,
    private val processBookingCommandHandler: ProcessBookingCommandHandler,
    private val completeBookingCommandHandler: CompleteBookingCommandHandler,
    private val failBookingCommandHandler: FailBookingCommandHandler,
    private val processCancelBookingCommandHandler: ProcessCancelBookingCommandHandler,
    private val cancelBookingCommandHandler: CancelBookingCommandHandler,
    private val failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private val log = LoggerFactory.getLogger(SagaEventHandler::class.java)
    }

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<BookingSagaStartedEvent> {
                scope.launch {
                    runCatching { onBookingSagaStartedEvent(processBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingSagaStartedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingSagaCompletedEvent> {
                scope.launch {
                    runCatching { onBookingSagaCompletedEvent(completeBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingSagaCompletedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingSagaFailedEvent> {
                scope.launch {
                    runCatching { onBookingSagaFailedEvent(failBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingSagaFailedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingCancelSagaStartedEvent> {
                scope.launch {
                    runCatching { onBookingCancelSagaStartedEvent(processCancelBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingCancelSagaStartedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingCancelSagaCompletedEvent> {
                scope.launch {
                    runCatching { onBookingCancelSagaCompletedEvent(cancelBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingCancelSagaCompletedEvent", e) }
                }
            }
        }
        scope.launch {
            eventBus.subscribe<BookingCancelSagaFailedEvent> {
                scope.launch {
                    runCatching { onBookingCancelSagaFailedEvent(failCancelBookingCommandHandler, it) }
                        .onFailure { e -> log.error("Error handling BookingCancelSagaFailedEvent", e) }
                }
            }
        }
    }
}

