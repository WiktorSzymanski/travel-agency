package pl.szymanski.wiktor.ta.saga

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.CompensateAccommodationCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CompensateCommuteCommand
import pl.szymanski.wiktor.ta.domain.Seat
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class CancelBookingSaga(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOffer: TravelOffer,
    private val seat: Seat,
    private val bookingId: BookingId,
    private val metadata: Metadata
) {
    private data class CancelContext(
        val commuteEventId: UUID? = null,
        val commuteSeat: Seat? = null,
        val accommodationEventId: UUID? = null,
    )

    private val accommodationCommand: AccommodationCommand =
        CancelAccommodationBookingCommand(
            travelOffer.accommodationId,
            metadata.correlationId,
            bookingId
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateCancelAccommodationBookingCommand(
            accommodationId = travelOffer.accommodationId,
            correlationId = metadata.correlationId,
            bookingId = bookingId
        )
    }

    private val commuteCommand: CommuteCommand =
        CancelCommuteBookingCommand(
            travelOffer.commuteId,
            metadata.correlationId,
            bookingId,
        )

    private fun getCompensateCommuteCommand(
        eventId: UUID,
        seat: Seat,
    ): CompensateCommuteCommand {
        return CompensateCancelCommuteBookingCommand(
            commuteId = travelOffer.commuteId,
            correlationId = metadata.correlationId,
            bookingId = bookingId,
            seat = seat,
        )
    }

    private val attractionCommand: AttractionCommand? =
        when (val attractionId = travelOffer.attractionId) {
            is AttractionId.Present -> CancelAttractionBookingCommand(
                attractionId,
                metadata.correlationId,
                bookingId
            )
            is AttractionId.Empty -> null
        }

    private val maxRetries = 5

    suspend fun execute() {
        eventBus.publish(
            EventEnvelope(
                BookingCancelSagaStartedEvent(
                    bookingId = bookingId,
                ),
                metadata
            )
        )

        val saga =
            Saga<CancelContext>()
                .addStep(
                    operation = { ctx ->
                        val (_, events) =
                            withRetry(maxRetries) {
                                commandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
                            }
                        val e = events.first() as CommuteBookingCanceledEvent
                        ctx.copy(commuteEventId = e.eventId, commuteSeat = e.seat)
                    },
                    compensation = { ctx ->
                        val evId = ctx.commuteEventId!!
                        val seat = ctx.commuteSeat!!

                        withRetry(maxRetries) {
                            commandBus.dispatch<CommuteCommand, Commute>(
                                getCompensateCommuteCommand(evId, seat),
                            )
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
                    ctx
                },
                compensation = { _ ->
                    // No-op: last step, nothing after attraction that could fail
                },
            )
        }

        val result = saga.process(CancelContext())

        if (result.isSuccess) {
            eventBus.publish(
                EventEnvelope(
                    BookingCancelSagaCompletedEvent(
                        bookingId = bookingId,
                        seat = seat,
                    ),
                    metadata
                )
            )
        } else {
            eventBus.publish(
                EventEnvelope(
                    BookingCancelSagaFailedEvent(
                        bookingId = bookingId,
                        message = result.exceptionOrNull()?.message ?: "Unknown error",
                    ),
                    metadata
                )
            )
        }
    }
}
