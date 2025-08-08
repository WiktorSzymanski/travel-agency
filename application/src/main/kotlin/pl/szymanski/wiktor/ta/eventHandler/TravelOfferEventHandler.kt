package pl.szymanski.wiktor.ta.eventHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.saga.BookingSaga
import pl.szymanski.wiktor.ta.saga.CancelBookingSaga
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.command.UpdateBookingStateCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCancelRequestedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReserveFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.event.CancelBookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.CancelBookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.CancelBookingSagaStartedEvent
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
        scope.launch { travelOfferReserveFailedEventHandler() }
        scope.launch { travelOfferBookFailedEventHandler() }
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
                scope.launch {
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
                scope.launch {
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
                scope.launch {
                    travelOfferExpireService.expireTravelOfferByCommute(it.commuteId, it.correlationId!!)
                }
            }
        }

    suspend fun accommodationExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AccommodationExpiredEvent> {
                scope.launch {
                    travelOfferExpireService.expireTravelOfferByAccommodation(it.accommodationId, it.correlationId!!)
                }
            }
        }

    suspend fun attractionExpiredEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<AttractionExpiredEvent> {
                scope.launch {
                    travelOfferExpireService.expireTravelOfferByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteFullEvent> {
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
            EventBus.subscribe<AttractionFullEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferUnavailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun commuteBookingCanceledEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) =
        coroutineScope {
            EventBus.subscribe<CommuteAvailableEvent> {
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
            EventBus.subscribe<AttractionAvailableEvent> {
                scope.launch {
                    travelOfferStatusService.makeTravelOfferAvailableByAttraction(it.attractionId, it.correlationId!!)
                }
            }
        }

    suspend fun bookingCreatedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCreatedEvent> {
            scope.launch {
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

    suspend fun travelOfferReserveFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<TravelOfferReserveFailedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.FAILED,
                        it.message
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun travelOfferBookFailedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<TravelOfferBookFailedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.FAILED,
                        it.message
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun bookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaStartedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.PROCESSING,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun bookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaCompletedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.SUCCEEDED,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun bookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingSagaCompletedEvent> {
            scope.launch {
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
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        state = BookingState.FAILED,
                        message = it.message
                    )
                )
            }
        }
    }

    suspend fun cancelBookingSagaStartedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<CancelBookingSagaStartedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.PROCESSING_CANCELLATION,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<CancelBookingSagaCompletedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        it.bookingId,
                        it.correlationId!!,
                        BookingState.CANCELED,
                    ) as BookingCommand,
                )
            }
        }
    }

    suspend fun cancelBookingSagaCompletedEventHandler2(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<CancelBookingSagaCompletedEvent> {
            scope.launch {
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
        EventBus.subscribe<CancelBookingSagaFailedEvent> {
            scope.launch {
                bookingCommandHandler.handle(
                    UpdateBookingStateCommand(
                        bookingId = it.bookingId,
                        correlationId = it.correlationId!!,
                        state = BookingState.SUCCEEDED,
                        message = it.message
                    )
                )
            }
        }
    }

    suspend fun bookingCancelRequestedEventHandler(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) = coroutineScope {
        EventBus.subscribe<BookingCancelRequestedEvent> {
            scope.launch {
                travelOfferCommandHandler.handle(
                    ReleaseTravelOfferCommand(
                        it.travelOfferId,
                        it.correlationId!!,
                        it.bookingId,
                        it.seat) as TravelOfferCommand,
                )
            }
        }
    }



}
