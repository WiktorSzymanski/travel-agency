package pl.szymanski.wiktor.ta

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommand
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.accommodation.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.accommodation.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.attraction.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.commute.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommand
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
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


    fun prepareCommands() = when (triggeringEvent) {
        is TravelOfferBookedEvent -> {
            accommodationCommand =
                BookAccommodationCommand(triggeringEvent.accommodationId, triggeringEvent.correlationId!!, triggeringEvent.userId)
            commuteCommand =
                BookCommuteCommand(triggeringEvent.commuteId, triggeringEvent.correlationId!!, triggeringEvent.userId, triggeringEvent.seat)
            attractionCommand =
                triggeringEvent.attractionId?.let {
                    BookAttractionCommand(it, triggeringEvent.correlationId!!, triggeringEvent.userId)
                }
        }
        is TravelOfferBookingCanceledEvent -> {
            accommodationCommand =
                CancelAccommodationBookingCommand(triggeringEvent.accommodationId, triggeringEvent.correlationId!!, triggeringEvent.userId)
            commuteCommand =
                CancelCommuteBookingCommand(triggeringEvent.commuteId, triggeringEvent.correlationId!!, triggeringEvent.userId, triggeringEvent.seat)
            attractionCommand =
                triggeringEvent.attractionId?.let {
                    CancelAttractionBookingCommand(it, triggeringEvent.correlationId!!, triggeringEvent.userId)
            }
        }
        else -> { throw IllegalArgumentException("Invalid event for BookingSaga: $triggeringEvent") }
    }

    suspend fun execute() =
        coroutineScope {
            prepareCommands()

            val handleJobs = mutableListOf<Deferred<Result<Any>>>()

            handleJobs +=
                async {
                    runCatching { commuteCommandHandler.handle(commuteCommand as CommuteCommand) }
                }

            handleJobs +=
                async {
                    runCatching { accommodationCommandHandler.handle(accommodationCommand as AccommodationCommand) }
                }

            attractionCommand?.let {
                handleJobs +=
                    async {
                        runCatching { attractionCommandHandler.handle(it as AttractionCommand) }
                    }
            }

            val results = handleJobs.awaitAll()

            if (results.any { it.isFailure }) {
                println("Saga Failed — running compensations")

                val successfulEvents =
                    results
                        .filter { it.isSuccess }
                        .mapNotNull { it.getOrNull() }
                        .reversed()

                successfulEvents.forEach { event ->
                    when (event) {
                        is CommuteEvent -> commuteCommandHandler.compensate(event)
                        is AccommodationEvent -> accommodationCommandHandler.compensate(event)
                        is AttractionEvent -> attractionCommandHandler.compensate(event)
                    }
                }

                travelOfferCommandHandler.compensate(triggeringEvent)
            } else {
                println("Saga Succeeded")
            }
        }
}
