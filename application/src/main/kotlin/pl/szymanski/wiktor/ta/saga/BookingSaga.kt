package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.coroutineScope
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
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class BookingSaga(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOfferService: TravelOfferService,
    private val triggeringEvent: TravelOfferReservedEvent,
    private val triggeringEventMetadata: Metadata
) {
    companion object {
        private const val DEFAULT_MAX_RETRIES = 30
    }
    private data class BookingContext(
        val commuteEventId: UUID? = null,
        val accommodationEventId: UUID? = null,
    )

    private val accommodationCommand: AccommodationCommand =
        BookAccommodationCommand(
            triggeringEvent.accommodationId,
            triggeringEventMetadata.correlationId,
            triggeringEvent.bookingId,
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateBookAccommodationCommand(
            accommodationId = triggeringEvent.accommodationId,
            correlationId = triggeringEventMetadata.correlationId,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private var commuteCommand: CommuteCommand =
        BookCommuteCommand(
            triggeringEvent.commuteId,
            triggeringEventMetadata.correlationId,
            triggeringEvent.bookingId,
            triggeringEvent.seat,
        )

    private fun getCompensateCommuteCommand(eventId: UUID): CompensateCommuteCommand {
        return CompensateBookCommuteCommand(
            commuteId = triggeringEvent.commuteId,
            correlationId = triggeringEventMetadata.correlationId,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = triggeringEvent.attractionId) {
            is AttractionId.Present -> BookAttractionCommand(
                attractionId,
                triggeringEventMetadata.correlationId,
                triggeringEvent.bookingId,
            )
            is AttractionId.Empty -> null
        }

    private val bookingId: BookingId = triggeringEvent.bookingId

    private val maxRetries = DEFAULT_MAX_RETRIES

    suspend fun execute() {
        eventBus.publish(
            EventEnvelope(
                BookingSagaStartedEvent(
                    bookingId = bookingId,
                ),
                triggeringEventMetadata
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
                                    getCompensateCommuteCommand(evId),
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
                                    getCompensateAccommodationCommand(evId),
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
                        travelOfferId = triggeringEvent.travelOfferId,
                        seat = triggeringEvent.seat,
                    ),
                    triggeringEventMetadata,
                )
            )
        } else {
            compensateTriggeringEvent(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    suspend fun compensateTriggeringEvent(message: String) =
        coroutineScope {
            eventBus.publish(
                EventEnvelope(
                    BookingSagaFailedEvent(
                        bookingId = bookingId,
                        message = message,
                    ),
                    triggeringEventMetadata,
                )
            )
//            withRetry(maxRetries) { travelOfferCommandHandler.compensate(triggeringEvent) }

            if (!travelOfferService
                    .checkTravelOfferComponentsAvailability(triggeringEvent.travelOfferId)
            ) {
                withRetry(maxRetries) {
//                    travelOfferCommandHandler.handle(
//                        MakeTravelOfferUnavailableCommand(
//                            triggeringEvent.travelOfferId,
//                            triggeringEvent.correlationId!!,
//                        ) as TravelOfferCommand,
//                    )
                }
            }
        }
}
