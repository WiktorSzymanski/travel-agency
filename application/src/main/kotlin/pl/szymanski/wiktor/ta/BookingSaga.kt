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
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent

class BookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
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
            is TravelOfferBookedEvent -> {
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
//
//    suspend fun <T> retryWithResults(
//        maxRetries: Int,
//        actions: List<Any>,
//        handler: suspend (action: Any) -> Result<T>,
//    ): List<Result<T>> {
//        val actionsLeft = actions.toMutableList()
//        var attempts = 0
//        var lastResults: List<Result<T>> = emptyList()
//
//        do {
//            attempts++
//            lastResults = actionsLeft.map { handler(it) }
//            lastResults.filter { it.isSuccess }.map { actionsLeft.remove(it.getOrNull()) }
//        } while (attempts < maxRetries)
//
//        return lastResults
//    }


    suspend fun execute() =
        coroutineScope {
            log.info("BookingSaga ${triggeringEvent.correlationId} status: executing")

            var attempts = 0
            var lastResults: List<Result<Any>>

            var runCommute = true
            var runAccom = true
            var runAttraction = attractionCommand != null

            do {
                attempts++

                lastResults = runJobs(
                    runCommutes = runCommute,
                    runAccommodations = runAccom,
                    runAttractions = runAttraction,
                )

                val failures = lastResults.withIndex().filter { it.value.isFailure }
                val allFailuresAreConcurrent = failures.isNotEmpty() &&
                        failures.all { it.value.exceptionOrNull() is ConcurrentModificationException }

                successJobs.addAll(lastResults.filter { it.isSuccess })

                runCommute = failures.any { it.index == 0 }
                runAccom   = failures.any { it.index == 1 }
                runAttraction = failures.any { it.index == 2 }

                if (!allFailuresAreConcurrent || failures.isEmpty()) break
                log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying some jobs due to ConcurrentModificationException, attempt: $attempts")
            } while (attempts < MAX_RETRIES)

            val numOfSuccessfulJobsNeeded = if (attractionCommand != null) 3 else 2

            if (successJobs.size < numOfSuccessfulJobsNeeded) {
                log.error("BookingSaga ${triggeringEvent.correlationId} status: Finished, result: Failed — running compensations")
                val successfulEvents =
                    successJobs
                        .mapNotNull { it.getOrNull() }
                        .reversed()

                // SHOULD CHECK IF COMPENSATION SUCCEDED

                var attempts = 0
                var lastResults: List<Result<Any>>
                val eventsToCompensate = successfulEvents.toMutableList()

                eventsToCompensate.add(triggeringEvent)


                do {
                    attempts++
                    val a = mutableListOf<Deferred<Result<Any>>>()
                    eventsToCompensate.forEach { event ->
                        when (event) {
                            is CommuteEvent -> a += async { runCatching { commuteCommandHandler.compensate(event) } }
                            is AccommodationEvent -> a += async { runCatching { accommodationCommandHandler.compensate(event) } }
                            is AttractionEvent -> a += async { runCatching { attractionCommandHandler.compensate(event) } }
                            is TravelOfferBookedEvent -> a += async { runCatching { travelOfferCommandHandler.compensate(event) } }
                        }
                    }

                    lastResults = a.awaitAll()
                    lastResults.filter { it.isSuccess }.map {
                        when(it.getOrNull()) {
                            is CommuteEvent -> eventsToCompensate.removeIf { e -> e is CommuteEvent }
                            is AccommodationEvent -> eventsToCompensate.removeIf { e -> e is AccommodationEvent }
                            is AttractionEvent -> eventsToCompensate.removeIf { e -> e is AttractionEvent }
                            is TravelOfferEvent -> eventsToCompensate.removeIf { e -> e is TravelOfferBookedEvent }
                          } }
                    if (eventsToCompensate.isEmpty()) break
                    log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying some compensation jobs, attempt: $attempts" +
                            "\n\tEvents to compensate: $eventsToCompensate" +
                            "\n\tReason of failure: ${lastResults.mapNotNull { it.exceptionOrNull()?.message }.toList()}")
                    // SOME DBOUNCE TIME?
                } while (attempts < MAX_RETRIES)
        } else {
            log.info("BookingSaga ${triggeringEvent.correlationId} status: Finished, result: Succeeded")
        }
    }
}
