package pl.szymanski.wiktor.ta.eventhandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.subscribe

class BookingEventHandler (
    private val eventBus: EventBus,
    private val commandBus: CommandBus) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun bookingCreatedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCreatedEvent> {
                scope.launchCatching {
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

    suspend fun bookingCancelRequestedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            eventBus.subscribe<BookingCancelRequestedEvent> {
                scope.launchCatching {
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
}
