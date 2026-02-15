package pl.szymanski.wiktor.ta.commandhandler

import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    suspend fun handle(command: AccommodationCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        when (command) {
            is CreateAccommodationCommand -> handle(command)
            is BookAccommodationCommand -> handle(command)
            is CancelAccommodationBookingCommand -> handle(command)
            is ExpireAccommodationCommand -> handle(command)
            is CompensateBookAccommodationCommand -> compensate(command)
            is CompensateCancelAccommodationBookingCommand -> compensate(command)
        }

    private fun handle(command: CreateAccommodationCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        ).let { Triple(it.first, it.second, Metadata(command.correlationId, 0)) }

    private suspend fun handle(command: BookAccommodationCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.first.book(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: CancelAccommodationBookingCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.first.cancelBooking(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun handle(command: ExpireAccommodationCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.first.expire()
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateBookAccommodationCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.first.compensateBook(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }

    private suspend fun compensate(command: CompensateCancelAccommodationBookingCommand): Triple<Accommodation, List<AccommodationEvent>, Metadata> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.first.compensateCancelBooking(command.bookingId)
                Triple(it.first, events, Metadata(command.correlationId, it.second + 1))
            }
}
