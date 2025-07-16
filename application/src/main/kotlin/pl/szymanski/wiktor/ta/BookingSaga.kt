package pl.szymanski.wiktor.ta

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommand
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.accommodation.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommand
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.commute.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommand
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.command.travelOffer.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent

class BookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val event: TravelOfferBookedEvent
) {
    suspend fun execute() = coroutineScope {
        val compensations = mutableListOf<suspend () -> Unit>()

        launch {
            EventBus.subscribe<CommuteBookedEvent>(event.correlationId!!) {
                compensations.add { commuteCommandHandler.compensate(it) }
            }
        }

        launch {
            EventBus.subscribe<AccommodationBookedEvent>(event.correlationId!!) {
                compensations.add { accommodationCommandHandler.compensate(it) }
            }
        }
        event.attractionId?.let {
            launch {
                EventBus.subscribe<AttractionBookedEvent>(event.correlationId!!) {
                    compensations.add { attractionCommandHandler.compensate(it) }
                }
            }
        }

        val accommodationCommand = BookAccommodationCommand(event.accommodationId, event.correlationId!!, event.userId)
        val commuteCommand = BookCommuteCommand(event.commuteId, event.correlationId!!, event.userId, event.seat)
        val attractionCommand = event.attractionId?.let {
            BookAttractionCommand(it, event.correlationId!!, event.userId)
        }

        val handleJobs = mutableListOf<Deferred<Result<Unit>>>()

        handleJobs += async {
            runCatching { commuteCommandHandler.handle(commuteCommand as CommuteCommand) }
        }

        handleJobs += async {
            runCatching { accommodationCommandHandler.handle(accommodationCommand as AccommodationCommand) }
        }

        attractionCommand?.let {
            handleJobs += async {
                runCatching { attractionCommandHandler.handle(it as AttractionCommand) }
            }
        }

        // Can't there be race between collecting all jobs and caching events of completed jobs?
        // Shouldn't handle return event in handleJobs list? Seems more robust
        if (handleJobs.awaitAll().any { it.isFailure }) {
            println("Saga Failed — running compensations")
            compensations.reversed().forEach { it.invoke() }
            travelOfferCommandHandler.compensate(event)
        } else {
            println("Saga Succeeded")
        }

        coroutineContext.cancelChildren()
    }
}