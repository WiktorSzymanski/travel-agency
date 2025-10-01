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
import pl.szymanski.wiktor.ta.withRetry

class AccommodationCommandHandler(
    private val accommodationRepository: AccommodationRepository,
) {
    val maxRetries = 1

    suspend fun handle(command: AccommodationCommand): AccommodationEvent =
        withRetry (maxRetries) {
            when (command) {
                is BookAccommodationCommand -> handle(command)
                is CancelAccommodationBookingCommand -> handle(command)
                is CreateAccommodationCommand -> handle(command)
                is ExpireAccommodationCommand -> handle(command)
            }.apply { first.correlationId = command.correlationId }
                .also { EventBus.publish(it.first, it.second, it.third) }.first
        }

    suspend fun handle(command: BookAccommodationCommand): Triple<AccommodationEvent, Long, String> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .book(command.bookingId)
                    .let { Triple(it, accommodation.lastRevision + 1L,accommodation.lastEtag!!) }
            }

    suspend fun handle(command: CancelAccommodationBookingCommand): Triple<AccommodationEvent, Long, String> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .cancelBooking(command.bookingId)
                    .let { Triple(it, accommodation.lastRevision + 1L,accommodation.lastEtag!!) }
            }

    suspend fun handle(command: CreateAccommodationCommand): Triple<AccommodationEvent, Long, String?> =
        Accommodation.create(
            command.name,
            command.location,
            command.rent,
        ).let { (accommodation, event) ->
            Triple(event, accommodation.lastRevision + 1L,accommodation.lastEtag)
        }

    suspend fun handle(command: ExpireAccommodationCommand): Triple<AccommodationEvent, Long, String> =
        accommodationRepository
            .findById(command.accommodationId)
            .let { accommodation ->
                accommodation
                    .expire()
                    .let { Triple(it, accommodation.lastRevision + 1L,accommodation.lastEtag!!) }
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
            }.also { EventBus.publish(it.first, it.second, it.third) }.first
        }


    suspend fun compensate(event: AccommodationBookedEvent): Triple<AccommodationEvent, Long, String> =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateBook(event.bookingId)
                    .let { Triple(it, accommodation.lastRevision + 1L,accommodation.lastEtag!!) }
            }

    suspend fun compensate(event: AccommodationBookingCanceledEvent): Triple<AccommodationEvent, Long, String> =
        accommodationRepository
            .findById(event.accommodationId)
            .let { accommodation ->
                accommodation
                    .compensateCancelBooking(event.bookingId)
                    .let { Triple(it, accommodation.lastRevision + 1L,accommodation.lastEtag!!) }
            }
}
