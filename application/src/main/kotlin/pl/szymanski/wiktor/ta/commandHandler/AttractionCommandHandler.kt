package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    suspend fun handle(command: AttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        when (command) {
            is CreateAttractionCommand -> handle(command)
            is BookAttractionCommand -> handle(command)
            is CancelAttractionBookingCommand -> handle(command)
            is ExpireAttractionCommand -> handle(command)
        }

    private fun handle(command: CreateAttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        )

    private suspend fun handle(command: BookAttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.book(command.bookingId)
                it to events
            }

    private suspend fun handle(command: CancelAttractionBookingCommand): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.cancelBooking(command.bookingId)
                it to events
            }

    private suspend fun handle(command: ExpireAttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.expire()
                it to events
            }

    suspend fun compensate(event: AttractionEvent): Pair<Attraction, List<AttractionEvent>> =
        when (event) {
            is AttractionBookedEvent -> compensate(event)
            is AttractionBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensateEvents = it.second.map { e -> e.toCompensation() }
            it.first to compensateEvents
        }

    private suspend fun compensate(event: AttractionBookedEvent): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(event.attractionId)
            .let {
                val events = it.compensateBook(event.bookingId)
                it to events
            }

    private suspend fun compensate(event: AttractionBookingCanceledEvent): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(event.attractionId)
            .let {
                val events = it.compensateCancelBooking(event.bookingId)
                it to events
            }
}
