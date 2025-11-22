package pl.szymanski.wiktor.ta.eventHandler

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

class BookingEventHandler {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferEventHandler::class.java)
    }

    suspend fun bookingCreatedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCreatedEvent> {
            scope.launchCatching {
                CommandBus.dispatch<TravelOfferCommand, TravelOffer>(
                    ReserveTravelOfferCommand(
                        it.travelOfferId,
                        it.correlationId!!,
                        it.bookingId,
                        it.seat,
                    ) as TravelOfferCommand,
                )
            }
        }
    }

    suspend fun bookingCancelRequestedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelRequestedEvent> {
            scope.launchCatching {
                runCatching {
                    CommandBus.dispatch<TravelOfferCommand, TravelOffer>(
                        ReleaseTravelOfferCommand(
                            it.travelOfferId,
                            it.correlationId!!,
                            it.bookingId,
                            it.seat) as TravelOfferCommand,
                    )
                }.onFailure { e ->
                    log.error("Failed to release travel offer event: $it", e)
                }
            }
        }
    }
}