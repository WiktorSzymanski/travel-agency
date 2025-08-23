package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelBookingCommand
import pl.szymanski.wiktor.ta.command.CompleteBookingCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessBookingCommand
import pl.szymanski.wiktor.ta.command.ProcessCancelBookingCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.launchCatching
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService

class TravelOfferEventHandler(
    private val travelOfferExpireService: TravelOfferExpireService,
    private val travelOfferStatusService: TravelOfferStatusService,
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val bookingCommandHandler: BookingCommandHandler,
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
        scope.launch { bookingCreatedEventHandler() }
//        scope.launch { travelOfferReserveFailedEventHandler() }
//        scope.launch { travelOfferBookFailedEventHandler() }
        scope.launch { bookingSagaStartedEventHandler() }
        scope.launch { bookingSagaCompletedEventHandler() }
        scope.launch { bookingSagaCompletedEventHandler2() }
        scope.launch { bookingSagaFailedEventHandler() }
        scope.launch { cancelBookingSagaStartedEventHandler() }
        scope.launch { cancelBookingSagaCompletedEventHandler() }
        scope.launch { cancelBookingSagaCompletedEventHandler2() }
        scope.launch { cancelBookingSagaFailedEventHandler() }
        scope.launch { bookingCancelRequestedEventHandler() }
    }

    suspend fun travelOfferReservedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferReservedEvent> {
                scope.launchCatching {
                    BookingSaga(
                        travelOfferCommandHandler,
                        attractionCommandHandler,
                        commuteCommandHandler,
                        accommodationCommandHandler,
                        travelOfferStatusService,
                        it,
                    ).execute()
                }
            }
        }

    suspend fun travelOfferReleaseEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<TravelOfferReleaseEvent> {
                scope.launchCatching {
                    CancelBookingSaga(
                        travelOfferCommandHandler,
                        attractionCommandHandler,
                        commuteCommandHandler,
                        accommodationCommandHandler,
                        travelOfferStatusService,
                        it,
                    ).execute()
                }
            }
        }

    suspend fun commuteExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteExpiredEvent> {
                scope.launchCatching {
                    travelOfferExpireService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                scope.launchCatching {
                    travelOfferExpireService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                scope.launchCatching {
                    travelOfferExpireService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteFullEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferUnavailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookedEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferUnavailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionFullEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferUnavailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteAvailableEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferAvailableByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationBookingCanceledEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferAvailableByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionAvailableEvent> {
                scope.launchCatching {
                    travelOfferStatusService.makeTravelOfferAvailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun bookingCreatedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCreatedEvent> {
            scope.launchCatching {
                travelOfferCommandHandler.handle(
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

//    suspend fun travelOfferReserveFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
//        EventBus.subscribe<TravelOfferReserveFailedEvent> {
//            scope.launchCatching {
//                bookingCommandHandler.handle(
//                    FailBookingCommand(
//                        it.bookingId,
//                        it.correlationId!!,
//                        it.message
//                    ) as BookingCommand,
//                )
//            }
//        }
//    }

//    suspend fun travelOfferBookFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
//        EventBus.subscribe<TravelOfferBookFailedEvent> {
//            scope.launchCatching {
//                bookingCommandHandler.handle(
//                    FailBookingCommand(
//                        it.bookingId,
//                        it.correlationId!!,
//                        it.message
//                    ) as BookingCommand,
//                )
//            }
//        }
//    }

    suspend fun bookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaStartedEvent> {
            scope.launchCatching {
                bookingCommandHandler.handle(
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
                bookingCommandHandler.handle(
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
                travelOfferCommandHandler.handle(
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
                bookingCommandHandler.handle(
                    FailBookingCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        message = it.message
                    )
                )
            }
        }
    }

    suspend fun cancelBookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaStartedEvent> {
            scope.launchCatching {
                bookingCommandHandler.handle(
                    ProcessCancelBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaCompletedEvent> {
            scope.launchCatching {
                bookingCommandHandler.handle(
                    CancelBookingCommand(
                        it.bookingId,
                        it.correlationId!!,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaCompletedEvent> {
            scope.launchCatching {
                travelOfferCommandHandler.handle(
                    CancelBookTravelOfferCommand(
                        travelOfferId = it.travelOfferId,
                        correlationId = it.correlationId!!,
                        bookingId = it.bookingId,
                        seat = it.seat
                    )
                )
            }
        }
    }

    suspend fun cancelBookingSagaFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelSagaFailedEvent> {
            scope.launchCatching {
                bookingCommandHandler.handle(
                    FailCancelBookingCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        message = it.message
                    )
                )
            }
        }
    }

    suspend fun bookingCancelRequestedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelRequestedEvent> {
            scope.launchCatching {
                runCatching {
                    travelOfferCommandHandler.handle(
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