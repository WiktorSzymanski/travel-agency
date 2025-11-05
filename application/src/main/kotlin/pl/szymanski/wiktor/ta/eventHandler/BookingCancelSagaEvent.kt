package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.launchCatching

class BookingCancelSagaEvent() {
    suspend fun cancelBookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaStartedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    ProcessCancelBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaCompletedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    CancelBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaCompletedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    CancelBookTravelOfferCommand(
                        travelOfferId = it.travelOfferId,
                        correlationId = it.correlationId!!,
                        bookingId = it.bookingId,
                        seat = it.seat
                    )
                )
            }
        }
    }

    suspend fun cancelBookingSagaFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaFailedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    FailCancelBookingCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        message = it.message
                    )
                )
            }
        }
    }
}