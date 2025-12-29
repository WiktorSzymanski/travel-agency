package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAttractionCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAttractionCommand
import pl.szymanski.wiktor.ta.command.ExpireAttractionCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    suspend fun handle(command: AttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        when (command) {
            is CreateAttractionCommand -> handle(command)
            is BookAttractionCommand -> handle(command)
            is CancelAttractionBookingCommand -> handle(command)
            is ExpireAttractionCommand -> handle(command)
            is CompensateBookAttractionCommand -> compensate(command)
            is CompensateCancelAttractionBookingCommand -> compensate(command)
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

    private suspend fun compensate(command: CompensateBookAttractionCommand): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.compensateBook(command.bookingId)
                it to events
            }

    private suspend fun compensate(command: CompensateCancelAttractionBookingCommand): Pair<Attraction, List<AttractionEvent>> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.compensateCancelBooking(command.bookingId)
                it to events
            }
}
