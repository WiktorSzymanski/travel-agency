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
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.subscribe

class BookingSagaEventHandler (
    private val eventBus: EventBus,
    private val commandBus: CommandBus) {
    suspend fun bookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingSagaStartedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        ProcessBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        ) as BookingCommand,
                    )
                }
            }
        }

    suspend fun bookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingSagaCompletedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        CompleteBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        ) as BookingCommand,
                    )
                }
            }
        }

    suspend fun bookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingSagaCompletedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        BookTravelOfferCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                            bookingId = it.event.bookingId,
                            seat = it.event.seat,
                        ) as TravelOfferCommand,
                    )
                }
            }
        }

    suspend fun bookingSagaFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingSagaFailedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        FailBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                            message = it.event.message,
                        ) as BookingCommand,
                    )
                }
            }
        }
}
