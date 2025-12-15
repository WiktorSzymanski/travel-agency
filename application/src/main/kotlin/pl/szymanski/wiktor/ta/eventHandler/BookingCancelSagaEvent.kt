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
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.subscribe

class BookingCancelSagaEvent (
    private val eventBus: EventBus,
    private val commandBus: CommandBus
) {
    suspend fun cancelBookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCancelSagaStartedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        ProcessCancelBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        ) as BookingCommand,
                    )
                }
            }
        }

    suspend fun cancelBookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCancelSagaCompletedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        CancelBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        ) as BookingCommand,
                    )
                }
            }
        }

    suspend fun cancelBookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCancelSagaCompletedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        CancelBookTravelOfferCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                            bookingId = it.event.bookingId,
                            seat = it.event.seat,
                        ) as TravelOfferCommand,
                    )
                }
            }
        }

    suspend fun cancelBookingSagaFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCancelSagaFailedEvent> {
                scope.launchCatching {
                    commandBus.dispatch<BookingCommand, Booking>(
                        FailCancelBookingCommand(
                            bookingId = it.event.bookingId,
                            correlationId = it.metadata.correlationId,
                            message = it.event.message,
                        ) as BookingCommand,
                    )
                }
            }
        }
}
