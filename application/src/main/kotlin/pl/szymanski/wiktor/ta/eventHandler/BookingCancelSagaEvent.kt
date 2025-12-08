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
                            it.bookingId,
                            it.correlationId!!,
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
                            it.bookingId,
                            it.correlationId!!,
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
                            travelOfferId = it.travelOfferId,
                            correlationId = it.correlationId!!,
                            bookingId = it.bookingId,
                            seat = it.seat,
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
                            bookingId = it.bookingId,
                            correlationId = it.correlationId!!,
                            message = it.message,
                        ) as BookingCommand,
                    )
                }
            }
        }
}
