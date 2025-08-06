package pl.szymanski.wiktor.ta

import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.command.UpdateBookingStateCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import java.util.UUID
import kotlin.reflect.KClass

class BookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val travelOfferStatusService: TravelOfferStatusService,
    private val bookingCommandHandler: BookingCommandHandler,
    private val triggeringEvent: TravelOfferEvent,
) {
    private lateinit var accommodationCommand: AccommodationCommand
    private lateinit var commuteCommand: CommuteCommand
    private var attractionCommand: AttractionCommand? = null

    private lateinit var finCommand: TravelOfferCommand

    private val maxRetries = 3

    init {
        log.info("New BookingSaga created for correlationId: {}", triggeringEvent.correlationId)
        prepareCommands()
    }

    companion object {
        private val log = LoggerFactory.getLogger(BookingSaga::class.java)
    }

    fun prepareCommands() =
        when (triggeringEvent) {
            is TravelOfferReservedEvent -> {
                accommodationCommand =
                    BookAccommodationCommand(
                        triggeringEvent.accommodationId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                    )
                commuteCommand =
                    BookCommuteCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                        triggeringEvent.seat,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        BookAttractionCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.bookingId,
                        )
                    }
                finCommand =
                    BookTravelOfferCommand(
                        correlationId = triggeringEvent.correlationId!!,
                        travelOfferId = triggeringEvent.travelOfferId,
                        bookingId = triggeringEvent.bookingId,
                        seat = triggeringEvent.seat,
                    ) as TravelOfferCommand
            }
            is TravelOfferReleaseEvent -> {
                accommodationCommand =
                    CancelAccommodationBookingCommand(
                        triggeringEvent.accommodationId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                    )
                commuteCommand =
                    CancelCommuteBookingCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        CancelAttractionBookingCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.bookingId,
                        )
                    }
            }
            is TravelOfferBookingCanceledEvent -> {
                accommodationCommand =
                    CancelAccommodationBookingCommand(
                        triggeringEvent.accommodationId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                    )
                commuteCommand =
                    CancelCommuteBookingCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.bookingId,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        CancelAttractionBookingCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.bookingId,
                        )
                    }
                finCommand =
                    CancelBookTravelOfferCommand(
                        correlationId = triggeringEvent.correlationId!!,
                        travelOfferId = triggeringEvent.travelOfferId,
                        bookingId = triggeringEvent.bookingId,
                        seat = triggeringEvent.seat,
                    ) as TravelOfferCommand
            }
            else -> {
                throw IllegalArgumentException("Invalid event for BookingSaga: $triggeringEvent")
            }
        }

//    suspend fun <T> sagaStep(
//        action: suspend (action: Any) -> Result<T>,
//        compensation: suspend (action: Any) -> Result<T>,
//    ) {
//        var attempts = 0
//        val sJobs = mutableListOf<Event>()
//        var lastJob: Result<Any>
//
//        do {
//            attempts++
//            val res = runCatching { commuteCommandHandler.handle(commuteCommand) }
//            lastJob = res
//
//            if (res.isSuccess) {
//                sJobs.add(res.getOrNull() as CommuteEvent)
//                break
//            }
//
//            if (res.exceptionOrNull() !is ConcurrentModificationException) {
//                break
//            }
//
//            log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying commute command execution due to ConcurrentModificationException, attempt: $attempts")
//        } while (attempts < MAX_RETRIES)
//    }

    suspend fun bookingUpdate(bookingId: UUID, status: BookingState, message: String? = null) {
        bookingCommandHandler.handle(
            UpdateBookingStateCommand(
                bookingId,
                triggeringEvent.correlationId!!,
                status,
                message,
            )
        )
    }

    suspend fun execute(): Boolean {
        log.info("BookingSaga ${triggeringEvent.correlationId} status: executing")

        val bookingId = when (triggeringEvent) {
            is TravelOfferReservedEvent -> triggeringEvent.bookingId
            is TravelOfferBookingCanceledEvent -> triggeringEvent.bookingId
            else -> throw IllegalArgumentException("Invalid event for BookingSaga: $triggeringEvent")
        }

        bookingUpdate(bookingId, BookingState.PROCESSING)

        var attempts = 0
        val sJobs = mutableListOf<Event>()
        var lastJob: Result<Any>

        do {
            attempts++
            val res = runCatching { commuteCommandHandler.handle(commuteCommand) }
            lastJob = res

            if (res.isSuccess) {
                sJobs.add(res.getOrNull() as CommuteEvent)
                break
            }

            if (res.exceptionOrNull() !is ConcurrentModificationException) {
                break
            }

            log.info(
                "BookingSaga ${triggeringEvent.correlationId} status: retrying commute" +
                    "command execution due to ConcurrentModificationException, attempt: $attempts",
            )
        } while (attempts < maxRetries)

        if (sJobs.isEmpty()) {
            log.error(
                "BookingSaga ${triggeringEvent.correlationId} status: Finished, result:" +
                    "Failed due to ${lastJob.exceptionOrNull()} — running compensating actions",
            )
            bookingUpdate(bookingId, BookingState.FAILED, lastJob.exceptionOrNull()?.message)
            compensateTriggeringEvent()
            return false
        }

        attempts = 0
        do {
            attempts++
            val res = runCatching { accommodationCommandHandler.handle(accommodationCommand) }
            lastJob = res

            if (res.isSuccess) {
                sJobs.add(res.getOrNull() as AccommodationEvent)
                break
            }

            if (res.exceptionOrNull() !is ConcurrentModificationException) {
                break
            }

            log.info(
                "BookingSaga ${triggeringEvent.correlationId} status: retrying accommodation" +
                    "command execution due to ConcurrentModificationException, attempt: $attempts",
            )
        } while (attempts < maxRetries)

        if (sJobs.size < 2) {
            log.error(
                "BookingSaga ${triggeringEvent.correlationId} status: Finished, result:" +
                    "Failed due to ${lastJob.exceptionOrNull()} — running compensating actions",
            )
            bookingUpdate(bookingId, BookingState.FAILED, lastJob.exceptionOrNull()?.message)
            attempts = 0
            do {
                attempts++
                val res = runCatching { commuteCommandHandler.compensate(sJobs.first() as CommuteEvent) }

                if (res.isFailure) {
                    if (attempts == maxRetries) {
                        log.error(
                            "BookingSaga ${triggeringEvent.correlationId} status:" +
                                "Failed to compensate commute command, result: $res",
                        )
                    }
                    continue
                }

                sJobs.removeAt(0)
                break
            } while (attempts < maxRetries)
            compensateTriggeringEvent()
            return false
        }

        if (attractionCommand != null) {
            attempts = 0
            do {
                attempts++
                val res = runCatching { attractionCommandHandler.handle(attractionCommand!!) }
                lastJob = res

                if (res.isSuccess) {
                    sJobs.add(res.getOrNull() as AttractionEvent)
                    break
                }

                if (res.exceptionOrNull() !is ConcurrentModificationException) {
                    break
                }

                log.info(
                    "BookingSaga ${triggeringEvent.correlationId} status: retrying attraction" +
                        "command execution due to ConcurrentModificationException, attempt: $attempts",
                )
            } while (attempts < maxRetries)

            if (sJobs.size < 3) {
                log.error(
                    "BookingSaga ${triggeringEvent.correlationId} status: Finished, result:" +
                        "Failed due to ${lastJob.exceptionOrNull()} — running compensating actions",
                )
                bookingUpdate(bookingId, BookingState.FAILED, lastJob.exceptionOrNull()?.message)
                attempts = 0
                do {
                    attempts++
                    val res = runCatching { accommodationCommandHandler.compensate(sJobs[1] as AccommodationEvent) }

                    if (res.isFailure) {
                        if (attempts == maxRetries) {
                            log.error(
                                "BookingSaga ${triggeringEvent.correlationId} status: Failed" +
                                    "to compensate accommodation command, result: $res",
                            )
                        }
                        continue
                    }

                    sJobs.removeAt(1)
                    break
                } while (attempts < maxRetries)

                attempts = 0
                do {
                    attempts++
                    val res = runCatching { commuteCommandHandler.compensate(sJobs.first() as CommuteEvent) }

                    if (res.isFailure) {
                        if (attempts == maxRetries) {
                            log.error(
                                "BookingSaga ${triggeringEvent.correlationId} status: Failed" +
                                    "to compensate commute command, result: $res",
                            )
                        }
                        continue
                    }

                    sJobs.removeAt(0)
                    break
                } while (attempts < maxRetries)
                compensateTriggeringEvent()
                return false
            }
        }

        val res = runCatching {
            withRetry(3) {
                travelOfferCommandHandler.handle(finCommand)
            }
        }

        if (res.isFailure) {
            //compensation here for all
            log.error("WHAT THE HELLY: {}", res.exceptionOrNull()?.message)
            bookingUpdate(bookingId, BookingState.FAILED, res.exceptionOrNull()?.message)
            return false
        }

        bookingUpdate(bookingId, BookingState.SUCCEEDED)
        return true
    }

    suspend fun <T> withRetry(
        maxRetries: Int,
        onException: KClass<out Exception> = Exception::class,
        action: suspend () -> T,
    ): T {
        var lastException: Throwable? = null
        repeat(maxRetries) {
            val res = runCatching { action() }
            if (res.isSuccess) return res.getOrThrow()

            lastException = res.exceptionOrNull()
            if (!onException.isInstance(lastException)) throw lastException!!
        }
        throw lastException ?: IllegalStateException("No attempt made")
    }

    suspend fun compensateTriggeringEvent() =
        coroutineScope {
            runCatching {
                withRetry(3) { travelOfferCommandHandler.compensate(triggeringEvent) }
            }.exceptionOrNull()?.let { log.error(
                "BookingSaga ${triggeringEvent.correlationId} status: Failed" +
                        "to compensate travelOfferBooked event, result: $it") }

            if (!travelOfferStatusService
                    .checkTravelOfferComponentsAvailability(triggeringEvent.travelOfferId)
            ) {
                runCatching {
                    withRetry(3) {
                        travelOfferCommandHandler.handle(
                            MakeTravelOfferUnavailableCommand(
                                triggeringEvent.travelOfferId,
                                triggeringEvent.correlationId!!,
                            ) as TravelOfferCommand,
                        )
                    }
                }.exceptionOrNull()?.let { log.error("ERROR HANDLED: {}", it.message) }
            }
        }
}
