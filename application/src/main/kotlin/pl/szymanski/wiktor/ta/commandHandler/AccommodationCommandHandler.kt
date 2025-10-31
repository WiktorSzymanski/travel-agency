package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
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
    suspend fun handle(command: AccommodationCommand): Pair<Accommodation, AccommodationEvent> =
        when (command) {
            is CreateAccommodationCommand -> handle(command)
            is BookAccommodationCommand -> handle(command)
            is CancelAccommodationBookingCommand -> handle(command)
            is ExpireAccommodationCommand -> handle(command)
        }

    private fun handle(command: CreateAccommodationCommand): Pair<Accommodation, AccommodationEvent> =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        )

    private suspend fun handle(command: BookAccommodationCommand): Pair<Accommodation, AccommodationEvent> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val event = it.book(command.bookingId)
                it to event
            }

    private suspend fun handle(command: CancelAccommodationBookingCommand): Pair<Accommodation, AccommodationEvent> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val event = it.cancelBooking(command.bookingId)
                it to event
            }

    private suspend fun handle(command: ExpireAccommodationCommand): Pair<Accommodation, AccommodationEvent> =
        accommodationRepository
            .findById(command.accommodationId)
            .let {
                val event = it.expire()
                it to event
            }

    suspend fun compensate(event: AccommodationEvent): Pair<Accommodation, AccommodationEvent> =
        when (event) {
            is AccommodationBookedEvent -> compensate(event)
            is AccommodationBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Non compensatable event type: ${event::class.simpleName}")
        }.let {
            val compensateEvent = it.second.toCompensation()
            it.first to compensateEvent
        }

    private suspend fun compensate(event: AccommodationBookedEvent): Pair<Accommodation, AccommodationEvent> =
        accommodationRepository
            .findById(event.accommodationId)
            .let {
                val event = it.compensateBook(event.bookingId)
                it to event
            }

    private suspend fun compensate(event: AccommodationBookingCanceledEvent): Pair<Accommodation, AccommodationEvent> =
        accommodationRepository
            .findById(event.accommodationId)
            .let {
                val event = it.compensateCancelBooking(event.bookingId)
                it to event
            }
}
