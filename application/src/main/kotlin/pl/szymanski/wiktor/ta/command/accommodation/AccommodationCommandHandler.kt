package pl.szymanski.wiktor.ta.command.accommodation

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.event.toCompensation

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    suspend fun handle(command: AccommodationCommand): AccommodationEvent =
        when (command) {
            is BookAccommodationCommand -> handle(command)
            is CancelAccommodationBookingCommand -> handle(command)
            is CreateAccommodationCommand -> handle(command)
            is ExpireAccommodationCommand -> handle(command)
        }.apply { correlationId = command.correlationId }.also { EventBus.publish(it) }

    suspend fun compensate(event: AccommodationEvent) {
        when (event) {
            is AccommodationBookedEvent ->
                handle(
                    CancelAccommodationBookingCommand(event.accommodationId, event.correlationId!!, event.userId),
                )

            is AccommodationBookingCanceledEvent ->
                handle(
                    BookAccommodationCommand(event.accommodationId, event.correlationId!!, event.userId),
                )

            else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
        }.apply { correlationId = event.correlationId }.toCompensation().also { EventBus.publish(it) }
    }

    suspend fun handle(command: BookAccommodationCommand): AccommodationEvent =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .book(command.userId)
                    .also { accommodationRepository.update(accommodation) }
            }.apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelAccommodationBookingCommand): AccommodationEvent =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .cancelBooking(command.userId)
                    .also { accommodationRepository.update(accommodation) }
            }.apply { correlationId = command.correlationId }

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
                    .also { accommodationRepository.update(accommodation) }
            }.apply { correlationId = command.correlationId }
}
