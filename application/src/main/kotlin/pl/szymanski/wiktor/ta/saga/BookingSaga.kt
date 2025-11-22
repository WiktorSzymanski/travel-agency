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
import pl.szymanski.wiktor.ta.command.CompensateBookAttractionCommand
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
    private var accommodationCommand: AccommodationCommand = BookAccommodationCommand(
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

    private var commuteCommand: CommuteCommand = BookCommuteCommand(
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

    private var attractionCommand: AttractionCommand? = triggeringEvent.attractionId?.let {
        BookAttractionCommand(
            it,
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
        )
    }

    private fun getCompensateAttractionCommand(eventId: UUID): AttractionCommand {
        return CompensateBookAttractionCommand(
            attractionId = triggeringEvent.attractionId!!,
            correlationId = triggeringEvent.correlationId!!,
            eventId = eventId,
            bookingId = triggeringEvent.bookingId,
        )
    }

    private var bookingId: UUID = triggeringEvent.bookingId


    private val maxRetries = 30


    suspend fun execute() {
        EventBus.ignoreRevisionPublish(
            BookingSagaStartedEvent(
                correlationId = triggeringEvent.correlationId!!,
                bookingId = bookingId,
            )
        )

        val cH = runCatching {
            withRetry(maxRetries) {
                CommandBus.dispatch<CommuteCommand, Commute>(commuteCommand)
            }
        }

        if (cH.isFailure) {
            compensateTriggeringEvent(cH.exceptionOrNull()?.message ?: "Unknown error")
            return
        }

        val (commute, commuteEvents) = cH.getOrThrow()

        val acH = runCatching {
            withRetry(maxRetries) {
                CommandBus.dispatch<AccommodationCommand, Accommodation>(accommodationCommand)
            }
        }

        if (acH.isFailure) {
            withRetry(maxRetries) {
                CommandBus.dispatch<CommuteCommand, Commute>(
                    getCompensateCommuteCommand(commuteEvents.first().eventId)
                )
            }
            compensateTriggeringEvent(acH.exceptionOrNull()?.message ?: "Unknown error")
            return
        }

        val (accommodation, accommodationEvents) = acH.getOrThrow()

        if (attractionCommand != null) {
            val atH = runCatching {
                withRetry(maxRetries) {
                    CommandBus.dispatch<AttractionCommand, Attraction>(attractionCommand!!)
                }
            }

            if (atH.isFailure) {
                withRetry(maxRetries) {
                    CommandBus.dispatch<AccommodationCommand, Accommodation>(
                        getCompensateAccommodationCommand(accommodationEvents.first().eventId)
                    )
                }
                withRetry(maxRetries) {
                    CommandBus.dispatch<CommuteCommand, Commute>(
                        getCompensateCommuteCommand(commuteEvents.first().eventId)
                    )
                }
                compensateTriggeringEvent(atH.exceptionOrNull()?.message ?: "Unknown error")
                return
            }

            val atHEvent = atH.getOrNull()
        }

        EventBus.ignoreRevisionPublish(
            BookingSagaCompletedEvent(
                correlationId = triggeringEvent.correlationId!!,
                bookingId = bookingId,
                travelOfferId = triggeringEvent.travelOfferId,
                seat = triggeringEvent.seat,
            )
        )
    }

    suspend fun compensateTriggeringEvent(message: String) =
        coroutineScope {
            EventBus.ignoreRevisionPublish(
                BookingSagaFailedEvent(
                    correlationId = triggeringEvent.correlationId!!,
                    bookingId = bookingId,
                    message = message
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
