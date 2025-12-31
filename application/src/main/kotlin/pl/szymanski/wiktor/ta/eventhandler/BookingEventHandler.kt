package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.subscribe

class BookingEventHandler (
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EventHandler(scope) {

    init {
        setupHandlers()
    }
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun bookingCreatedEventHandler() =
        eventBus.subscribe<BookingCreatedEvent> {
            coroutineScope {
                launch {
                    commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        ReserveTravelOfferCommand(
                            it.event.travelOfferId,
                            it.metadata.correlationId,
                            it.event.bookingId,
                            it.event.seat,
                        )
                    )
                }
            }
        }

    suspend fun bookingCancelRequestedEventHandler() =
        eventBus.subscribe<BookingCancelRequestedEvent> {
            coroutineScope {
                launch {
                    runCatching {
                        commandBus.dispatch<TravelOfferCommand, TravelOffer>(
                            ReleaseTravelOfferCommand(
                                it.event.travelOfferId,
                                it.metadata.correlationId,
                                it.event.bookingId,
                                it.event.seat,
                            )
                        )
                    }.onFailure { e ->
                        log.error("Failed to release travel offer event: $it", e)
                    }
                }
            }
        }

    override fun getHandlers(): List<suspend () -> Unit> = listOf(
        { bookingCreatedEventHandler() },
        { bookingCancelRequestedEventHandler() },
    )
}
