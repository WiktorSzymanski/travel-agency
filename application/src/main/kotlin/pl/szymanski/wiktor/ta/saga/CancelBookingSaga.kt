package pl.szymanski.wiktor.ta.saga

import kotlinx.coroutines.coroutineScope
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.AccommodationCommand
import pl.szymanski.wiktor.ta.command.AttractionCommand
import pl.szymanski.wiktor.ta.command.CancelAccommodationBookingCommand
import pl.szymanski.wiktor.ta.command.CancelAttractionBookingCommand
import pl.szymanski.wiktor.ta.command.CancelCommuteBookingCommand
import pl.szymanski.wiktor.ta.command.CommuteCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaCompletedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaFailedEvent
import pl.szymanski.wiktor.ta.event.BookingCancelSagaStartedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferService
import pl.szymanski.wiktor.ta.withRetry
import java.util.*

class CancelBookingSaga(
    private val travelOfferCommandHandler: TravelOfferCommandHandler,
    private val attractionCommandHandler: AttractionCommandHandler,
    private val commuteCommandHandler: CommuteCommandHandler,
    private val accommodationCommandHandler: AccommodationCommandHandler,
    private val travelOfferService: TravelOfferService,
    private val triggeringEvent: TravelOfferReleaseEvent,
) {
    private var accommodationCommand: AccommodationCommand = CancelAccommodationBookingCommand(
        triggeringEvent.accommodationId,
        triggeringEvent.correlationId!!,
        triggeringEvent.bookingId,
    )
    private var commuteCommand: CommuteCommand = CancelCommuteBookingCommand(
        triggeringEvent.commuteId,
        triggeringEvent.correlationId!!,
        triggeringEvent.bookingId,
    )
    private var attractionCommand: AttractionCommand? =
        triggeringEvent.attractionId?.let {
            CancelAttractionBookingCommand(
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
//            log.info("CancelBookingSaga ${triggeringEvent.correlationId} status: retrying commute command execution due to ConcurrentModificationException, attempt: $attempts")
//        } while (attempts < MAX_RETRIES)
//    }

    suspend fun execute() {
        EventBus.ignoreRevisionPublish(
            BookingCancelSagaStartedEvent(
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

        val acH = runCatching {
            withRetry(maxRetries) {
                accommodationCommandHandler.handle(accommodationCommand)
            }
        }

        if (acH.isFailure) {
            withRetry(maxRetries) {
//                commuteCommandHandler.compensate(cHEvent as CommuteEvent)
            }
            compensateTriggeringEvent(acH.exceptionOrNull()?.message ?: "Unknown error")
            return
        }

        val acHEvent = acH.getOrNull()

        if (attractionCommand != null) {
            val atH = runCatching {
                withRetry(maxRetries) {
                    attractionCommandHandler.handle(attractionCommand!!)
                }
            }

            if (atH.isFailure) {
                withRetry(maxRetries) {
//                    commuteCommandHandler.compensate(cHEvent as CommuteEvent)
                }
                withRetry(maxRetries) {
//                    accommodationCommandHandler.compensate(acHEvent as AccommodationEvent)
                }
                compensateTriggeringEvent(atH.exceptionOrNull()?.message ?: "Unknown error")
                return
            }

            val atHEvent = atH.getOrNull()
        }

        // Co jeśli nie wiadomo czemu BOOK się wywali
        EventBus.ignoreRevisionPublish(
            BookingCancelSagaCompletedEvent(
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
                BookingCancelSagaFailedEvent(
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
