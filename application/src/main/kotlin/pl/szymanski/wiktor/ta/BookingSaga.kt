package pl.szymanski.wiktor.ta

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import kotlin.reflect.KClass

class BookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val travelOfferStatusService: TravelOfferStatusService,
    private val triggeringEvent: TravelOfferEvent,
) {
    private lateinit var accommodationCommand: AccommodationCommand
    private lateinit var commuteCommand: CommuteCommand
    private var attractionCommand: AttractionCommand? = null

    val successJobs = mutableListOf<Result<Any>>()

    private val MAX_RETRIES = 3

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
                        triggeringEvent.userId,
                    )
                commuteCommand =
                    BookCommuteCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.userId,
                        triggeringEvent.seat,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        BookAttractionCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.userId,
                        )
                    }
            }
            is TravelOfferReleaseEvent -> {
                accommodationCommand =
                    CancelAccommodationBookingCommand(
                        triggeringEvent.accommodationId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.userId,
                    )
                commuteCommand =
                    CancelCommuteBookingCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.userId,
                        triggeringEvent.seat,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        CancelAttractionBookingCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.userId,
                        )
                    }
            }
            is TravelOfferBookingCanceledEvent -> {
                accommodationCommand =
                    CancelAccommodationBookingCommand(
                        triggeringEvent.accommodationId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.userId,
                    )
                commuteCommand =
                    CancelCommuteBookingCommand(
                        triggeringEvent.commuteId,
                        triggeringEvent.correlationId!!,
                        triggeringEvent.userId,
                        triggeringEvent.seat,
                    )
                attractionCommand =
                    triggeringEvent.attractionId?.let {
                        CancelAttractionBookingCommand(
                            it,
                            triggeringEvent.correlationId!!,
                            triggeringEvent.userId,
                        )
                    }
            }
            else -> {
                throw IllegalArgumentException("Invalid event for BookingSaga: $triggeringEvent")
            }
        }

    suspend fun runJobs(
        runCommutes: Boolean = true,
        runAccommodations: Boolean = true,
        runAttractions: Boolean = attractionCommand != null
    ): List<Result<Any>> = coroutineScope {
        val handleJobs = mutableListOf<Deferred<Result<Any>>>()
        if (runCommutes) handleJobs +=
            async {
                runCatching { commuteCommandHandler.handle(commuteCommand) }
            }

        if (runAccommodations) handleJobs +=
            async {
                runCatching { accommodationCommandHandler.handle(accommodationCommand) }
            }

        if (runAttractions) handleJobs +=
            async {
                runCatching { attractionCommandHandler.handle(attractionCommand!!) }
            }

        return@coroutineScope handleJobs.awaitAll()
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

    suspend fun execute() : Boolean {
            log.info("BookingSaga ${triggeringEvent.correlationId} status: executing")

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

                log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying commute command execution due to ConcurrentModificationException, attempt: $attempts")
            } while (attempts < MAX_RETRIES)

            if (sJobs.isEmpty()) {
                log.error("BookingSaga ${triggeringEvent.correlationId} status: Finished, result: Failed due to ${lastJob.exceptionOrNull()} — running compensating actions")
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

                log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying accommodation command execution due to ConcurrentModificationException, attempt: $attempts")
            } while (attempts < MAX_RETRIES)

            if (sJobs.size < 2) {
                log.error("BookingSaga ${triggeringEvent.correlationId} status: Finished, result: Failed due to ${lastJob.exceptionOrNull()} — running compensating actions")
                attempts = 0
                do {
                    attempts++
                    val res = runCatching { commuteCommandHandler.compensate(sJobs.first() as CommuteEvent) }

                    if (res.isFailure) {
                        if (attempts == MAX_RETRIES) log.error("BookingSaga ${triggeringEvent.correlationId} status: Failed to compensate commute command, result: $res")
                        continue
                    }

                    sJobs.removeAt(0)
                    break
                } while (attempts < MAX_RETRIES)
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

                    log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying attraction command execution due to ConcurrentModificationException, attempt: $attempts")
                } while (attempts < MAX_RETRIES)

                if (sJobs.size < 3) {
                    log.error("BookingSaga ${triggeringEvent.correlationId} status: Finished, result: Failed due to ${lastJob.exceptionOrNull()} — running compensating actions")
                    attempts = 0
                    do {
                        attempts++
                        val res = runCatching { accommodationCommandHandler.compensate(sJobs[1] as AccommodationEvent) }

                        if (res.isFailure) {
                            if (attempts == MAX_RETRIES) log.error("BookingSaga ${triggeringEvent.correlationId} status: Failed to compensate accommodation command, result: $res")
                            continue
                        }

                        sJobs.removeAt(1)
                        break
                    } while (attempts < MAX_RETRIES)

                    attempts = 0
                    do {
                        attempts++
                        val res = runCatching { commuteCommandHandler.compensate(sJobs.first() as CommuteEvent) }

                        if (res.isFailure) {
                            if (attempts == MAX_RETRIES) log.error("BookingSaga ${triggeringEvent.correlationId} status: Failed to compensate commute command, result: $res")
                            continue
                        }

                        sJobs.removeAt(0)
                        break
                    } while (attempts < MAX_RETRIES)
                    compensateTriggeringEvent()
                    return false
                }
            }

            return true
        }

    suspend fun <T> withRetry(
        maxRetries: Int,
        onException: KClass<out Exception> = Exception::class,
        action: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        repeat(maxRetries) {
            try {
                return action()
            } catch (e: Exception) {
                if (!onException.isInstance(e)) {
                    throw e
                }
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("No attempt made")
    }

    suspend fun compensateTriggeringEvent() =
        coroutineScope {
            var attempts = 0
            do {
                attempts++
                val res = runCatching { travelOfferCommandHandler.compensate(triggeringEvent) }

                if (res.isFailure) {
                    if (attempts == MAX_RETRIES) log.error("BookingSaga ${triggeringEvent.correlationId} status: Failed to compensate travelOfferBooked event, result: $res")
                    continue
                }

                break
            } while (attempts < MAX_RETRIES)

            if (!travelOfferStatusService.checkTravelOfferComponentsAvailability(triggeringEvent.travelOfferId)) {
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
