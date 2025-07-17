package pl.szymanski.wiktor.ta.command.accommodation

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.event.toCompensationEvent

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    // TODO: all should be handled by this but for now saga sucks so it is what is is
    suspend fun handle(command: AccommodationCommand) {
        EventBus.publish(
            when (command) {
                is BookAccommodationCommand -> handle(command)
                is CancelAccommodationBookingCommand -> handle(command)
            }.apply { correlationId = command.correlationId },
        )
    }

    suspend fun compensate(event: AccommodationEvent) {
        EventBus.publish(
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
            }.apply { correlationId = event.correlationId }.toCompensationEvent(),
        )
    }

    suspend fun handle(command: BookAccommodationCommand): AccommodationEvent =
        accommodationRepository.findById(command.accommodationId).let { accommodation ->
            accommodation.book(command.userId)
                .also { accommodationRepository.update(accommodation) }
        }.apply { correlationId = command.correlationId }

    suspend fun handle(command: CancelAccommodationBookingCommand): AccommodationEvent =
        accommodationRepository.findById(command.accommodationId).let { accommodation ->
            accommodation.cancelBooking(command.userId)
                .also { accommodationRepository.update(accommodation) }
        }.apply { correlationId = command.correlationId }
}
