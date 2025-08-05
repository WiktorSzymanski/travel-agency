package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.BookingSaga
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService

class TravelOfferEventHandler(
    private val travelOfferExpireService: TravelOfferExpireService,
    private val travelOfferStatusService: TravelOfferStatusService,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferEventHandler::class.java)
    }

    fun setup(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        scope.launch { travelOfferReservedEventHandler() }
        scope.launch { travelOfferReleaseEventHandler() }
        scope.launch { commuteExpiredEventHandler() }
        scope.launch { accommodationExpiredEventHandler() }
        scope.launch { attractionExpiredEventHandler() }
        scope.launch { commuteBookedEventHandler() }
        scope.launch { accommodationBookedEventHandler() }
        scope.launch { attractionBookedEventHandler() }
        scope.launch { commuteBookingCanceledEventHandler() }
        scope.launch { accommodationBookingCanceledEventHandler() }
        scope.launch { attractionBookingCanceledEventHandler() }
    }

    suspend fun travelOfferReservedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferReservedEvent> {
                scope.launch {
                    log.info("New travel offer reserved: {}", it.travelOfferId)
                    BookingSaga(
                        travelOfferCommandHandler,
                        attractionCommandHandler,
                        commuteCommandHandler,
                        accommodationCommandHandler,
                        travelOfferStatusService,
                        it,
                    ).execute().let { bool ->
                        if (bool) {
                            travelOfferCommandHandler.handle(
                                BookTravelOfferCommand(
                                    correlationId = it.correlationId!!,
                                    travelOfferId = it.travelOfferId,
                                    userId = it.userId,
                                    seat = it.seat,
                                ) as TravelOfferCommand,
                            )
                        }
                    }
                }
            }
        }

    suspend fun travelOfferReleaseEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferReleaseEvent> {
                scope.launch {
                    log.info("Travel offer releasing: {}", it.travelOfferId)
                    BookingSaga(
                        travelOfferCommandHandler,
                        attractionCommandHandler,
                        commuteCommandHandler,
                        accommodationCommandHandler,
                        travelOfferStatusService,
                        it,
                    ).execute().let { bool ->
                        if (bool) {
                            travelOfferCommandHandler.handle(
                                CancelBookTravelOfferCommand(
                                    correlationId = it.correlationId!!,
                                    travelOfferId = it.travelOfferId,
                                    userId = it.userId,
                                    seat = it.seat,
                                ) as TravelOfferCommand,
                            )
                        }
                    }
                }
            }
        }

    suspend fun commuteExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to commute expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to accommodation expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                scope.launch {
                    log.info("TravelOffer expire due to attraction expired event: {}", it)
                    travelOfferExpireService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteBookedEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferUnavailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookedEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferUnavailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionBookedEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferUnavailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteBookingCanceledEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferAvailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookingCanceledEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferAvailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionBookingCanceledEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferAvailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }
}
