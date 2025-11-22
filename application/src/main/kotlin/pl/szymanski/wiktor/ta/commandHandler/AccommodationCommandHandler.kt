package pl.szymanski.wiktor.ta.commandHandler

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
import pl.szymanski.wiktor.ta.event.toCompensation

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    suspend fun handle(command: AccommodationCommand): Pair<Accommodation, List<AccommodationEvent>> =
        when (command) {
            is CreateAccommodationCommand -> handle(command)
            is BookAccommodationCommand -> handle(command)
            is CancelAccommodationBookingCommand -> handle(command)
            is ExpireAccommodationCommand -> handle(command)
            is CompensateBookAccommodationCommand -> compensate(command)
            is CompensateCancelAccommodationBookingCommand -> compensate(command)
        }

    private fun handle(command: CreateAccommodationCommand): Pair<Accommodation, List<AccommodationEvent>> =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        )

    private suspend fun handle(command: BookAccommodationCommand): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.book(command.bookingId)
                it to events
            }

    private suspend fun handle(command: CancelAccommodationBookingCommand): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.cancelBooking(command.bookingId)
                it to events
            }

    private suspend fun handle(command: ExpireAccommodationCommand): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.expire()
                it to events
            }

    private suspend fun compensate(command: CompensateBookAccommodationCommand): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.compensateBook(command.bookingId)
                it to events.map { ev -> ev.toCompensation() }
            }

    private suspend fun compensate(command: CompensateCancelAccommodationBookingCommand): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val events = it.compensateCancelBooking(command.bookingId)
                it to events.map { ev -> ev.toCompensation() }
            }
}
