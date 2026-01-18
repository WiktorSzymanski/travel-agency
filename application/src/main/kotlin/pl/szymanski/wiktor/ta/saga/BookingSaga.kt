package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateBookCommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class BookingSaga(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOffer: TravelOffer,
    private val seat: Seat,
    private val bookingId: BookingId,
    private val metadata: Metadata
) {
    private data class BookingContext(
        val commuteEventId: UUID? = null,
        val accommodationEventId: UUID? = null,
    )

    private val accommodationCommand: AccommodationCommand =
        BookAccommodationCommand(
            travelOffer.accommodationId,
            metadata.correlationId,
            bookingId,
        )

    private fun getCompensateAccommodationCommand(): CompensateAccommodationCommand {
        return CompensateBookAccommodationCommand(
            accommodationId = travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            bookingId = bookingId,
        )
    }

    private var commuteCommand: CommuteCommand =
        BookCommuteCommand(
            travelOffer.commuteId,
            metadata.correlationId,
            bookingId,
            seat,
        )

    private fun getCompensateCommuteCommand(): CompensateCommuteCommand {
        return CompensateBookCommuteCommand(
            commuteId = travelOffer.commuteId,
            correlationId = metadata.correlationId,
            bookingId = bookingId,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = travelOffer.attractionId) {
            is AttractionId.Present -> BookAttractionCommand(
                attractionId,
                metadata.correlationId,
                bookingId,
            )
            is AttractionId.Empty -> null
        }

    private val maxRetries = 5

    suspend fun execute() {
        eventBus.publish(
            EventEnvelope(
                BookingSagaStartedEvent(
                    bookingId = bookingId,
                ),
                metadata
            )
        )

        val saga =
            Saga<BookingContext>()
                .addStep(
                    operation = { ctx ->
                        val (_, events) =
                            withRetry(maxRetries) {
                                commandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
                            }
                        ctx.copy(commuteEventId = events.first().eventId)
                    },
                    compensation = { ctx ->
                        ctx.commuteEventId?.let { evId ->
                            withRetry(maxRetries) {
                                commandBus.dispatch<CommuteCommand, Commute>(
                                    getCompensateCommuteCommand(),
                                )
                            }
                        }
                    },
                )
                .addStep(
                    operation = { ctx ->
                        val (_, events) =
                            withRetry(maxRetries) {
                                commandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand)
                            }
                        ctx.copy(accommodationEventId = events.first().eventId)
                    },
                    compensation = { ctx ->
                        ctx.accommodationEventId?.let { evId ->
                            withRetry(maxRetries) {
                                commandBus.dispatch<AccommodationCommand, Accommodation>(
                                    getCompensateAccommodationCommand(),
                                )
                            }
                        }
                    },
                )

        if (attractionCommand != null) {
            saga.addStep(
                operation = { ctx ->
                    withRetry(maxRetries) {
                        commandBus.dispatch<AttractionCommand, Attraction>(attractionCommand)
                    }
                    ctx // no need to mutate context
                },
                compensation = { _ ->
                    // No-op: there is no subsequent step that could fail after attraction
                },
            )
        }

        val result = saga.process(BookingContext())

        if (result.isSuccess) {
            eventBus.publish(
                EventEnvelope(
                    BookingSagaCompletedEvent(
                        bookingId = bookingId,
                        seat = seat,
                    ),
                    metadata
                )
            )
        } else {
            eventBus.publish(
                EventEnvelope(
                    BookingSagaFailedEvent(
                        bookingId = bookingId,
                        message = result.exceptionOrNull()?.message ?: "Unknown error",
                    ),
                    metadata
                )
            )
        }
    }
}
