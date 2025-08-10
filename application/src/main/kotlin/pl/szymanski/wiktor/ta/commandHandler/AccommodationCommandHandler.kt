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
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    val maxRetries = 10

    suspend fun handle(command: AccommodationCommand): AccommodationEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAccommodationCommand -> handle(command)
                is CancelAccommodationBookingCommand -> handle(command)
                is CreateAccommodationCommand -> handle(command)
                is ExpireAccommodationCommand -> handle(command)
            }
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    suspend fun handle(command: BookAccommodationCommand): AccommodationEvent =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .book(command.bookingId)
                    .also {
                        if (it !is AccommodationFailedEvent)
                            accommodationRepository.update(accommodation)
                    }
            }

    suspend fun handle(command: CancelAccommodationBookingCommand): AccommodationEvent =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .cancelBooking(command.bookingId)
                    .also {
                        if (it !is AccommodationFailedEvent)
                            accommodationRepository.update(accommodation)
                    }
            }

    suspend fun handle(command: CreateAccommodationCommand): AccommodationEvent =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        ).let { (accommodation, event) ->
            accommodationRepository.save(accommodation)
            event
        }

    suspend fun handle(command: ExpireAccommodationCommand): AccommodationEvent =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .expire()
                    .also {
                        if (it !is AccommodationFailedEvent)
                            accommodationRepository.update(accommodation)
                    }
            }

    suspend fun compensate(event: AccommodationEvent): AccommodationEvent =
        when (event) {
            is AccommodationBookedEvent -> compensate(event)
            is AccommodationBookingCanceledEvent -> compensate(event)
            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }

    suspend fun compensate(event: AccommodationBookedEvent): AccommodationEvent =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateBook(event.bookingId)
                    .also {
                        if (it !is AccommodationFailedEvent)
                            accommodationRepository.update(accommodation)
                    }
            }

    suspend fun compensate(event: AccommodationBookingCanceledEvent): AccommodationEvent =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateCancelBooking(event.bookingId)
                    .also {
                        if (it !is AccommodationFailedEvent)
                            accommodationRepository.update(accommodation)
                    }
            }
}
