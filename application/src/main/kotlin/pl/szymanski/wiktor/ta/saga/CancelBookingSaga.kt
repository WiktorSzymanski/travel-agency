package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
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
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class CancelBookingSaga(
    private val eventBus: EventBus,
    private val commandBus: CommandBus,
    private val travelOfferService: TravelOfferService,
    private val triggeringEvent: TravelOfferReleaseEvent,
) {
    companion object {
        private const val DEFAULT_MAX_RETRIES = 30
    }
    private data class CancelContext(
        val commuteEventId: UUID? = null,
        val commuteSeat: Seat? = null,
        val accommodationEventId: UUID? = null,
    )

    private val accommodationCommand: AccommodationCommand =
        CancelAccommodationBookingCommand(
            triggeringEvent.accommodationId,
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
        )

    private fun getCompensateAccommodationCommand(eventId: UUID): CompensateAccommodationCommand {
        return CompensateCancelAccommodationBookingCommand(
            accommodationId = triggeringEvent.accommodationId,
            correlationId = triggeringEvent.correlationId!!,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private val commuteCommand: CommuteCommand =
        CancelCommuteBookingCommand(
            triggeringEvent.commuteId,
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
        )

    private fun getCompensateCommuteCommand(
        eventId: UUID,
        seat: Seat,
    ): CompensateCommuteCommand {
        return CompensateCancelCommuteBookingCommand(
            commuteId = triggeringEvent.commuteId,
            correlationId = triggeringEvent.correlationId!!,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
            seat = seat,
        )
    }

    private val attractionCommand: AttractionCommand? =
        triggeringEvent.attractionId?.let {
            CancelAttractionBookingCommand(
                it,
                triggeringEvent.correlationId!!,
                triggeringEvent.bookingId,
            )
        }

    private val bookingId: UUID = triggeringEvent.bookingId

    private val maxRetries = DEFAULT_MAX_RETRIES

    suspend fun execute() {
        eventBus.publish(
            BookingCancelSagaStartedEvent(
                correlationId = triggeringEvent.correlationId!!,
                bookingId = bookingId,
            ),
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
                BookingCancelSagaCompletedEvent(
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
            eventBus.publish(
                BookingCancelSagaFailedEvent(
                    correlationId = triggeringEvent.correlationId!!,
                    bookingId = bookingId,
                    message = message,
                ),
            )
            // withRetry(maxRetries) { travelOfferCommandHandler.compensate(triggeringEvent) }

            if (!travelOfferService
                    .checkTravelOfferComponentsAvailability(triggeringEvent.travelOfferId)
            ) {
                withRetry(maxRetries) {
                    // travelOfferCommandHandler.handle(
                    //     MakeTravelOfferUnavailableCommand(
                    //         triggeringEvent.travelOfferId,
                    //         triggeringEvent.correlationId!!,
                    //     ) as TravelOfferCommand,
                    // )
                }
            }
        }
}
