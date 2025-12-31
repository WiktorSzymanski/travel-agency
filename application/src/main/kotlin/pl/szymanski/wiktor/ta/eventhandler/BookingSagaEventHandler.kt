package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import pl.szymanski.wiktor.ta.subscribe

class BookingSagaEventHandler (
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {

    init {
        setupHandlers()
    }
    suspend fun bookingSagaStartedEventHandler() =
        eventBus.subscribe<BookingSagaStartedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        ProcessBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun bookingSagaCompletedEventHandler() =
        eventBus.subscribe<BookingSagaCompletedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        CompleteBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                        )
                    )
                }
            }
        }

    suspend fun bookingSagaCompletedEventHandler2() =
        eventBus.subscribe<BookingSagaCompletedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        BookTravelOfferCommand(
                            it.event.travelOfferId,
                            it.metadata.correlationId,
                            bookingId = it.event.bookingId,
                            seat = it.event.seat,
                        )
                    )
                }
            }
        }

    suspend fun bookingSagaFailedEventHandler() =
        eventBus.subscribe<BookingSagaFailedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<BookingCommand, Booking>(
                        FailBookingCommand(
                            it.event.bookingId,
                            it.metadata.correlationId,
                            message = it.event.message,
                        )
                    )
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { bookingSagaStartedEventHandler() },
        { bookingSagaCompletedEventHandler() },
        { bookingSagaCompletedEventHandler2() },
        { bookingSagaFailedEventHandler() },
    )
}
