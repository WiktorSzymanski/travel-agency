package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CreateAccommodationCommand
import pl.szymanski.wiktor.ta.command.ExpireAccommodationCommand
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
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

    suspend fun compensate(event: AccommodationEvent): Pair<Accommodation, List<AccommodationEvent>> =
        when (event) {
            is AccommodationBookedEvent -> compensate(event)
            is AccommodationBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensationEvents = it.second.map { ev -> ev.toCompensation() }
            it.first to compensationEvents
        }

    private suspend fun compensate(event: AccommodationBookedEvent): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(event.accommodationId)
            .let {
                val events = it.compensateBook(event.bookingId)
                it to events
            }

    private suspend fun compensate(event: AccommodationBookingCanceledEvent): Pair<Accommodation, List<AccommodationEvent>> =
        accommodationRepository
            .findById(event.accommodationId)
            .let {
                val events = it.compensateCancelBooking(event.bookingId)
                it to events
            }
}
