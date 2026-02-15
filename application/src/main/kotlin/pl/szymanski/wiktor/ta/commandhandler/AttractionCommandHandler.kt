package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.Metadata
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
import java.util.UUID

class AttractionCommandHandler(
    private val attractionRepository: AttractionRepository,
) {
    suspend fun handle(command: AttractionCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        when (command) {
            is CreateAttractionCommand -> handle(command)
            is BookAttractionCommand -> handle(command)
            is CancelAttractionBookingCommand -> handle(command)
            is ExpireAttractionCommand -> handle(command)
            is CompensateBookAttractionCommand -> compensate(command)
            is CompensateCancelAttractionBookingCommand -> compensate(command)
        }

    private fun handle(command: CreateAttractionCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        Attraction.create(
            command.name,
            command.location,
            command.date,
            command.capacity,
        ).let { Triple(it.first, it.second, Metadata(command.correlationId, 0)) }

    private suspend fun handle(command: BookAttractionCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.first.book(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: CancelAttractionBookingCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.first.cancelBooking(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: ExpireAttractionCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.first.expire()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateBookAttractionCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.first.compensateBook(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateCancelAttractionBookingCommand): Triple<Attraction, List<AttractionEvent>, Metadata> =
        attractionRepository
            .findById(command.attractionId)
            .let {
                val events = it.first.compensateCancelBooking(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }
}
