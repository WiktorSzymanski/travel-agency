package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
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
    private val travelOfferService: TravelOfferService,
    private val triggeringEvent: TravelOfferReservedEvent,
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
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateBookAccommodationCommand(
            accommodationId = triggeringEvent.accommodationId,
            correlationId = triggeringEvent.correlationId!!,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private var commuteCommand: CommuteCommand =
        BookCommuteCommand(
            triggeringEvent.commuteId,
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
            triggeringEvent.seat,
        )

    private fun getCompensateCommuteCommand(eventId: UUID): CompensateCommuteCommand {
        return CompensateBookCommuteCommand(
            commuteId = triggeringEvent.commuteId,
            correlationId = triggeringEvent.correlationId!!,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private val attractionCommand: AttractionCommand? =
        triggeringEvent.attractionId?.let {
            BookAttractionCommand(
                it,
                triggeringEvent.correlationId!!,
                triggeringEvent.bookingId,
            )
        }

    private val bookingId: UUID = triggeringEvent.bookingId

    private val maxRetries = DEFAULT_MAX_RETRIES

    suspend fun execute() {
        EventBus.ignoreRevisionPublish(
            BookingSagaStartedEvent(
                correlationId = triggeringEvent.correlationId!!,
                bookingId = bookingId,
            ),
        )

        val saga =
            Saga<BookingContext>()
                .addStep(
                    operation = { ctx ->
                        val (_, events) =
                            withRetry(maxRetries) {
                                CommandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
                            }
                        ctx.copy(commuteEventId = events.first().eventId)
                    },
                    compensation = { ctx ->
                        ctx.commuteEventId?.let { evId ->
                            withRetry(maxRetries) {
                                CommandBus.dispatch<CommuteCommand, Commute>(
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
                                CommandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand)
                            }
                        ctx.copy(accommodationEventId = events.first().eventId)
                    },
                    compensation = { ctx ->
                        ctx.accommodationEventId?.let { evId ->
                            withRetry(maxRetries) {
                                CommandBus.dispatch<AccommodationCommand, Accommodation>(
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
                        CommandBus.dispatch<AttractionCommand, Attraction>(attractionCommand)
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
            EventBus.ignoreRevisionPublish(
                BookingSagaCompletedEvent(
                    correlationId = triggeringEvent.correlationId!!,
                    bookingId = bookingId,
                    travelOfferId = triggeringEvent.travelOfferId,
                    seat = triggeringEvent.seat,
                ),
            )
        } else {
            compensateTriggeringEvent(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    suspend fun compensateTriggeringEvent(message: String) =
        coroutineScope {
            EventBus.ignoreRevisionPublish(
                BookingSagaFailedEvent(
                    correlationId = triggeringEvent.correlationId!!,
                    bookingId = bookingId,
                    message = message,
                ),
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
