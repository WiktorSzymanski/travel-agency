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

    init {
        log.info("New BookingSaga created for event: {}", triggeringEvent)
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

    suspend fun execute() =
        coroutineScope {
            log.info("Executing BookingSaga for event: {}", triggeringEvent)

            val handleJobs = mutableListOf<Deferred<Result<Any>>>()

            handleJobs +=
                async {
                    runCatching { commuteCommandHandler.handle(commuteCommand) }
                }

            handleJobs +=
                async {
                    runCatching { accommodationCommandHandler.handle(accommodationCommand) }
                }

            attractionCommand?.let {
                handleJobs +=
                    async {
                        runCatching { attractionCommandHandler.handle(it) }
                    }
            }

            val results = handleJobs.awaitAll()

            if (results.any { it.isFailure }) {
                log.error("Saga result for travelOfferId ${triggeringEvent.travelOfferId}: Failed — running compensations")

                val successfulEvents =
                    results
                        .filter { it.isSuccess }
                        .mapNotNull { it.getOrNull() }
                        .reversed()

                // SHOULD CHECK IF COMPENSATION SUCCEDED

                successfulEvents.forEach { event ->
                    when (event) {
                        is CommuteEvent -> commuteCommandHandler.compensate(event)
                        is AccommodationEvent -> accommodationCommandHandler.compensate(event)
                        is AttractionEvent -> attractionCommandHandler.compensate(event)
                    }
                }

                travelOfferCommandHandler.compensate(triggeringEvent)
            } else {
                log.info("Saga result for travelOfferId ${triggeringEvent.travelOfferId}: Succeeded")
            }
        }
}
