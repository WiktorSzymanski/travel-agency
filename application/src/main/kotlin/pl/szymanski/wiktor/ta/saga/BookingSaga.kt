package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.BookAccommodationCommand
import pl.szymanski.wiktor.ta.command.BookAttractionCommand
import pl.szymanski.wiktor.ta.command.BookCommuteCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationFailedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFailedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFailedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingSagaStartedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class BookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val travelOfferStatusService: TravelOfferStatusService,
    private val triggeringEvent: TravelOfferReservedEvent,
) {
    private var accommodationCommand: AccommodationCommand = BookAccommodationCommand(
        triggeringEvent.accommodationId,
        triggeringEvent.correlationId!!,
        triggeringEvent.bookingId,
    )

    private var commuteCommand: CommuteCommand = BookCommuteCommand(
        triggeringEvent.commuteId,
        triggeringEvent.correlationId!!,
        triggeringEvent.bookingId,
        triggeringEvent.seat,
    )

    private var attractionCommand: AttractionCommand? = triggeringEvent.attractionId?.let {
        BookAttractionCommand(
            it,
            triggeringEvent.correlationId!!,
            triggeringEvent.bookingId,
        )
    }

    private var bookingId: UUID = triggeringEvent.bookingId


    private val maxRetries = 30


//    suspend fun <T> sagaStep(
//        action: suspend (action: Any) -> Result<T>,
//        compensation: suspend (action: Any) -> Result<T>,
//    ) {
//        var attempts = 0
//        val sJobs = mutableListOf<Event>()
//        var lastJob: Result<Any>
//
//        do {
//            attempts++
//            val res = runCatching { commuteCommandHandler.handle(commuteCommand) }
//            lastJob = res
//
//            if (res.isSuccess) {
//                sJobs.add(res.getOrNull() as CommuteEvent)
//                break
//            }
//
//            if (res.exceptionOrNull() !is ConcurrentModificationException) {
//                break
//            }
//
//            log.info("BookingSaga ${triggeringEvent.correlationId} status: retrying commute command execution due to ConcurrentModificationException, attempt: $attempts")
//        } while (attempts < MAX_RETRIES)
//    }

    suspend fun execute() {
        EventBus.ignoreRevisionPublish(
            BookingSagaStartedEvent(
                correlationId = triggeringEvent.correlationId!!,
                bookingId = bookingId,
            )
        )

        val cH = runCatching {
            withRetry(maxRetries) {
                commuteCommandHandler.handle(commuteCommand)
            }
        }

        if (cH.isFailure) {
            compensateTriggeringEvent(cH.exceptionOrNull()?.message ?: "Unknown error")
            return
        }

        val cHEvent = cH.getOrNull()
        if (cHEvent is CommuteFailedEvent) {
            compensateTriggeringEvent(cHEvent.message)
            return
        }

        val acH = runCatching {
            withRetry(maxRetries) {
                accommodationCommandHandler.handle(accommodationCommand)
            }
        }

        if (acH.isFailure) {
            withRetry(maxRetries) {
                commuteCommandHandler.compensate(cHEvent as CommuteEvent)
            }
            compensateTriggeringEvent(acH.exceptionOrNull()?.message ?: "Unknown error")
            return
        }

        val acHEvent = acH.getOrNull()
        if (acHEvent is AccommodationFailedEvent) {
            withRetry(maxRetries) {
                commuteCommandHandler.compensate(cHEvent as CommuteEvent)
            }
            compensateTriggeringEvent(acHEvent.message)
            return
        }

        if (attractionCommand != null) {
            val atH = runCatching {
                withRetry(maxRetries) {
                    attractionCommandHandler.handle(attractionCommand!!)
                }
            }

            if (atH.isFailure) {
                withRetry(maxRetries) {
                    commuteCommandHandler.compensate(cHEvent as CommuteEvent)
                }
                withRetry(maxRetries) {
                    accommodationCommandHandler.compensate(acHEvent as AccommodationEvent)
                }
                compensateTriggeringEvent(atH.exceptionOrNull()?.message ?: "Unknown error")
                return
            }

            val atHEvent = atH.getOrNull()
            if (atHEvent is AttractionFailedEvent) {
                withRetry(maxRetries) {
                    commuteCommandHandler.compensate(cHEvent as CommuteEvent)
                }
                withRetry(maxRetries) {
                    accommodationCommandHandler.compensate(acHEvent as AccommodationEvent)
                }
                compensateTriggeringEvent(atHEvent.message)
                return
            }
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
            withRetry(maxRetries) { travelOfferCommandHandler.compensate(triggeringEvent) }

            if (!travelOfferStatusService
                .checkTravelOfferComponentsAvailability(triggeringEvent.travelOfferId)
            ) {
                withRetry(maxRetries) {
                    travelOfferCommandHandler.handle(
                        MakeTravelOfferUnavailableCommand(
                            triggeringEvent.travelOfferId,
                            triggeringEvent.correlationId!!,
                        ) as TravelOfferCommand,
                    )
                }
            }
        }
}
