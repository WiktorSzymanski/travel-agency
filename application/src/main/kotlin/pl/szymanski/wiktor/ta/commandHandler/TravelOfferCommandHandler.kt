package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.BookingCommand
import pl.szymanski.wiktor.ta.command.CancelBookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CancelReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.CreateTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ExpireTravelOfferCommand
import pl.szymanski.wiktor.ta.command.FailBookingCommand
import pl.szymanski.wiktor.ta.command.FailCancelBookingCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferAvailableCommand
import pl.szymanski.wiktor.ta.command.MakeTravelOfferUnavailableCommand
import pl.szymanski.wiktor.ta.command.RebookTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReleaseTravelOfferCommand
import pl.szymanski.wiktor.ta.command.ReserveTravelOfferCommand
import pl.szymanski.wiktor.ta.command.TravelOfferCommand
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.event.toCompensation
import pl.szymanski.wiktor.ta.withRetry

interface QueueObject {
    fun sendBookingCommand(command: BookingCommand)
}

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
    private val queueClientProvider: QueueObject
) {
    val maxRetries = 1

    suspend fun handle(command: TravelOfferCommand): TravelOfferEvent? =
        withRetry(maxRetries) {
            when (command) {
                is BookTravelOfferCommand -> handle(command)
                is CancelBookTravelOfferCommand -> handle(command)
                is ReserveTravelOfferCommand -> handle(command)
                is ReleaseTravelOfferCommand -> handle(command)
                is RebookTravelOfferCommand -> handle(command)
                is CancelReserveTravelOfferCommand -> handle(command)
                is CreateTravelOfferCommand -> handle(command)
                is ExpireTravelOfferCommand -> handle(command)
                is MakeTravelOfferAvailableCommand -> handle(command)
                is MakeTravelOfferUnavailableCommand -> handle(command)
            }.apply { first.correlationId = command.correlationId }
                .also { EventBus.publish(it.first, it.second.toLong(), it.third) }.first
        }
    private suspend fun handle(command: CreateTravelOfferCommand): Triple<TravelOfferEvent, Int, String?> =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        ).let { (travelOffer, event) ->
            Triple(event, travelOffer.lastRevision, travelOffer.lastEtag)
        }

    private suspend fun handle(command: BookTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .book(command.bookingId, command.seat)
                        .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
                }.onFailure {
                    queueClientProvider.sendBookingCommand(
                        FailBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: CancelBookTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .cancelBooking(command.bookingId, command.seat)
                        .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
                }.onFailure {
                    queueClientProvider.sendBookingCommand(
                        FailCancelBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: ReleaseTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .releaseBooking(command.bookingId, command.seat)
                        .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
                }.onFailure {
                    queueClientProvider.sendBookingCommand(
                        FailCancelBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: RebookTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .rebook(command.bookingId)
                    .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
            }

    private suspend fun handle(command: ExpireTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .expire()
                    .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
            }

    private suspend fun handle(command: MakeTravelOfferUnavailableCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeUnavailable()
                    .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
            }

    private suspend fun handle(command: MakeTravelOfferAvailableCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeAvailable()
                    .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
            }

    private suspend fun handle(command: ReserveTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .reserve(command.bookingId, command.seat)
                        .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
                }.onFailure {
                    queueClientProvider.sendBookingCommand(
                        FailBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: CancelReserveTravelOfferCommand): Triple<TravelOfferEvent, Int, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelReservation(command.bookingId, command.seat)
                    .let { Triple(it, travelOffer.lastRevision, travelOffer.lastEtag!!) }
            }

    suspend fun compensate(event: TravelOfferEvent): TravelOfferEvent? =
        withRetry(maxRetries) {
            when (event) {
                is TravelOfferBookedEvent -> compensate(event)
                is TravelOfferBookingCanceledEvent -> compensate(event)
                is TravelOfferReservedEvent -> compensate(event)
                is TravelOfferReservationCanceledEvent -> compensate(event)
                is TravelOfferReleaseEvent -> compensate(event)
                else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
            }.let {
                it.first.correlationId = event.correlationId
                it.first.toCompensation()
                it
            }.also { EventBus.publish(it.first, it.second.toLong(), it.third) }.first
        }

    private suspend fun compensate(event: TravelOfferReleaseEvent): Triple<TravelOfferEvent, Int, String> =
        handle(
            RebookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
            )
        )

    private suspend fun compensate(event: TravelOfferBookedEvent): Triple<TravelOfferEvent, Int, String> =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferBookingCanceledEvent): Triple<TravelOfferEvent, Int, String> =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservedEvent): Triple<TravelOfferEvent, Int, String> =
        handle(
            CancelReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservationCanceledEvent): Triple<TravelOfferEvent, Int, String> =
        handle(
            ReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )
}
