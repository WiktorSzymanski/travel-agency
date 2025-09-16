package pl.szymanski.wiktor.ta.commandHandler

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.command.BookTravelOfferCommand
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

class TravelOfferCommandHandler(
    private val travelOfferRepository: TravelOfferRepository,
) {
    val maxRetries = 30

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
            }.apply { first?.correlationId = command.correlationId }
                .also { if (it.first != null) EventBus.publish(it.first!!, it.second) }.first
        }
    private suspend fun handle(command: CreateTravelOfferCommand): Pair<TravelOfferEvent, String?> =
        TravelOffer.create(
            command.name,
            command.commuteId,
            command.accommodationId,
            command.attractionId,
        ).let { (travelOffer, event) ->
            event to travelOffer.lastEtag
        }

    private suspend fun handle(command: BookTravelOfferCommand): Pair<TravelOfferEvent?, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .book(command.bookingId, command.seat)
                        .let { it to travelOffer.lastEtag!! }
                }.onFailure {
                    CommandBus.dispatch(
                        FailBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: CancelBookTravelOfferCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .cancelBooking(command.bookingId, command.seat)
                        .let { it to travelOffer.lastEtag!! }
                }.onFailure {
                    CommandBus.dispatch(
                        FailCancelBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: ReleaseTravelOfferCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .releaseBooking(command.bookingId, command.seat)
                        .let { it to travelOffer.lastEtag!! }
                }.onFailure {
                    CommandBus.dispatch(
                        FailCancelBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: RebookTravelOfferCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .rebook(command.bookingId)
                    .let { it to travelOffer.lastEtag!! }
            }

    private suspend fun handle(command: ExpireTravelOfferCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .expire()
                    .let { it to travelOffer.lastEtag!! }
            }

    private suspend fun handle(command: MakeTravelOfferUnavailableCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeUnavailable()
                    .let { it to travelOffer.lastEtag!! }
            }

    private suspend fun handle(command: MakeTravelOfferAvailableCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .makeAvailable()
                    .let { it to travelOffer.lastEtag!! }
            }

    private suspend fun handle(command: ReserveTravelOfferCommand): Pair<TravelOfferEvent?, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                runCatching {
                    travelOffer
                        .reserve(command.bookingId, command.seat)
                        .let { it to travelOffer.lastEtag!! }
                }.onFailure {
                    CommandBus.dispatch(
                        FailBookingCommand(
                            command.bookingId,
                            command.correlationId,
                            it.message
                        )
                    )
                }.getOrThrow()
            }

    private suspend fun handle(command: CancelReserveTravelOfferCommand): Pair<TravelOfferEvent, String> =
        travelOfferRepository
            .findById(command.travelOfferId)
            .let { travelOffer ->
                travelOffer
                    .cancelReservation(command.bookingId, command.seat)
                    .let { it to travelOffer.lastEtag!! }
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
                it.first?.correlationId = event.correlationId
                it.first?.toCompensation()
                it
            }.also { if (it.first != null) EventBus.publish(it.first!!, it.second) }.first
        }

    private suspend fun compensate(event: TravelOfferReleaseEvent): Pair<TravelOfferEvent, String> =
        handle(
            RebookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
            )
        )

    private suspend fun compensate(event: TravelOfferBookedEvent): Pair<TravelOfferEvent, String> =
        handle(
            CancelBookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferBookingCanceledEvent): Pair<TravelOfferEvent?, String> =
        handle(
            BookTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservedEvent): Pair<TravelOfferEvent, String> =
        handle(
            CancelReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )

    private suspend fun compensate(event: TravelOfferReservationCanceledEvent): Pair<TravelOfferEvent?, String> =
        handle(
            ReserveTravelOfferCommand(
                event.travelOfferId,
                event.correlationId!!,
                event.bookingId,
                event.seat,
            ),
        )
}
