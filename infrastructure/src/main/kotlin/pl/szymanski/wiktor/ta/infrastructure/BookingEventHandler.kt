//package pl.szymanski.wiktor.ta.infrastructure
//
//import jakarta.annotation.PostConstruct
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
//import org.springframework.stereotype.Service
//import pl.szymanski.wiktor.ta.CommandBus
//import pl.szymanski.wiktor.ta.EventBus
//import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
//import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
//import pl.szymanski.wiktor.ta.eventHandlerLogic.BookingEventHandleLogic
//import pl.szymanski.wiktor.ta.saga.SagaRepository
//import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
//import pl.szymanski.wiktor.ta.outbox.OutboxPort
//import pl.szymanski.wiktor.ta.saga.SagaState
//import pl.szymanski.wiktor.ta.subscribe
//
//@Service
//class BookingEventHandler(
//    private val eventBus: EventBus,
//) {
//    private val handleLogic: BookingEventHandleLogic? by lazy {
//        if (sagaRepository != null && sagaOutboxPort != null && deadLetterQueueRepository != null) {
//            BookingEventHandleLogic(
//                commandBus = commandBus,
//                sagaRepository = sagaRepository,
//                sagaOutboxPort = sagaOutboxPort,
//                deadLetterQueueRepository = deadLetterQueueRepository,
//            )
//        } else {
//            null
//        }
//    }
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
//
//    @PostConstruct
//    fun init() {
//        if (handleLogic != null) {
//            scope.launch {
//                eventBus.subscribe<BookingCreatedEvent> { event ->
//                    handleLogic?.onCreatedEvent(event)
//                }
//            }
//            scope.launch {
//                eventBus.subscribe<BookingCancelRequestedEvent> { event ->
//                    handleLogic?.onCancelRequestedEvent(event)
//                }
//            }
//        }
//    }
//}
//
//
//
//
//
