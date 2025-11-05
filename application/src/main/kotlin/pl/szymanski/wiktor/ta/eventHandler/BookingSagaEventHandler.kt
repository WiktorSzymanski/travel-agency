package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.launchCatching

class BookingSagaEventHandler() {
    suspend fun bookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaStartedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    ProcessBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun bookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaCompletedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    CompleteBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun bookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaCompletedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    BookTravelOfferCommand(
                        travelOfferId = it.travelOfferId,
                        correlationId = it.correlationId!!,
                        bookingId = it.bookingId,
                        seat = it.seat
                    )
                )
            }
        }
    }

    suspend fun bookingSagaFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaFailedEvent> {
            scope.launchCatching {
                CommandBus.dispatch(
                    FailBookingCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        message = it.message
                    )
                )
            }
        }
    }
}