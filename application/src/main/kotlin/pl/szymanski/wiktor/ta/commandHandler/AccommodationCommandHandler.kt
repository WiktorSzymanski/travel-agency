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
import pl.szymanski.wiktor.ta.domain.event.AccommodationFailedEvent
import pl.szymanski.wiktor.ta.domain.event.FailedEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    val maxRetries = 20

    suspend fun handle(command: AccommodationCommand): AccommodationEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAccommodationCommand -> handle(command)
                is CancelAccommodationBookingCommand -> handle(command)
                is CreateAccommodationCommand -> handle(command)
                is ExpireAccommodationCommand -> handle(command)
            }.apply { first.correlationId = command.correlationId }
                .also { EventBus.publish(it.first, it.second) }.first
        }

    suspend fun handle(command: BookAccommodationCommand): Pair<AccommodationEvent, Int> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .book(command.bookingId)
                    .let { it to accommodation.lastRevision }
            }

    suspend fun handle(command: CancelAccommodationBookingCommand): Pair<AccommodationEvent, Int> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .cancelBooking(command.bookingId)
                    .let { it to accommodation.lastRevision }
            }

    suspend fun handle(command: CreateAccommodationCommand): Pair<AccommodationEvent, Int> =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        ).let { (accommodation, event) ->
            event to accommodation.lastRevision
        }

    suspend fun handle(command: ExpireAccommodationCommand): Pair<AccommodationEvent, Int> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .expire()
                    .let { it to accommodation.lastRevision }
            }

    suspend fun compensate(event: AccommodationEvent): AccommodationEvent =
        withRetry(maxRetries) {
            when (event) {
                is AccommodationBookedEvent -> compensate(event)
                is AccommodationBookingCanceledEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it.first.correlationId = event.correlationId
                it.first.toCompensation()
                it
            }.also { if (it.first !is FailedEvent) EventBus.publish(it.first, it.second) }.first
        }


    suspend fun compensate(event: AccommodationBookedEvent): Pair<AccommodationEvent, Int> =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateBook(event.bookingId)
                    .let { it to accommodation.lastRevision }
            }

    suspend fun compensate(event: AccommodationBookingCanceledEvent): Pair<AccommodationEvent, Int> =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateCancelBooking(event.bookingId)
                    .let { it to accommodation.lastRevision }
            }
}
