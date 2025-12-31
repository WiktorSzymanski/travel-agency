package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import pl.szymanski.wiktor.ta.subscribe

class BookingCancelSagaEventHandler (
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {

    init {
        setupHandlers()
    }
    suspend fun cancelBookingSagaStartedEventHandler() =
        eventBus.subscribe<BookingCancelSagaStartedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        ProcessCancelBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun cancelBookingSagaCompletedEventHandler() =
        eventBus.subscribe<BookingCancelSagaCompletedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        CancelBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun cancelBookingSagaCompletedEventHandler2() =
        eventBus.subscribe<BookingCancelSagaCompletedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        CancelBookTravelOfferCommand(
                            it.event.travelOfferId,
                            it.metadata.correlationId,
                            bookingId = it.event.bookingId,
                            seat = it.event.seat,
                        )
                    )
                }
            }
        }

    suspend fun cancelBookingSagaFailedEventHandler() =
        eventBus.subscribe<BookingCancelSagaFailedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        FailCancelBookingCommand(
                            bookingId = it.event.bookingId,
                            correlationId = it.metadata.correlationId,
                            message = it.event.message,
                        )
                    )
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { cancelBookingSagaStartedEventHandler() },
        { cancelBookingSagaCompletedEventHandler() },
        { cancelBookingSagaCompletedEventHandler2() },
        { cancelBookingSagaFailedEventHandler() },
    )
}
