package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToStreamOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferMadeUnavailableEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferRebookedEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReleaseEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservationCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.TravelOfferReservedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.TravelOfferBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.travelOfferEventTypeRegistry
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdate
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateStatus

class TravelOfferProjectionService(
    private val kurrentDBClient: KurrentDBClient,
    private val travelOfferQueryRepository: TravelOfferQueryRepository
) {
    fun startProjection() {
        val streamName = "\$ce-travelOffer"
        val scope = CoroutineScope(Dispatchers.Default)

        val subscriptionOptions = SubscribeToStreamOptions.get()
            .fromStart()
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                scope.launch {
                    val eventTypeName = resolvedEvent.event.eventType
                    val eventClass = travelOfferEventTypeRegistry[eventTypeName]
                        ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

                    updateProjection(EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass))
                }
            }
        }

        println("Subscribing to $streamName")

        kurrentDBClient.subscribeToStream(streamName, listener, subscriptionOptions)
    }

    private suspend fun updateProjection(event: TravelOfferEvent) {
        when (event) {
            is TravelOfferCreatedEvent -> {
                travelOfferQueryRepository.save(
                    pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer(
                        _id = event.travelOfferId,
                        name = event.name,
                        commuteId = event.commuteId,
                        accommodationId = event.accommodationId,
                        attractionId = event.attractionId,
                        status = TravelOfferStatusEnum.AVAILABLE
                    )
                )
            }
            is TravelOfferReservedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.RESERVED,
                        bookingId = event.bookingId,
                    )
                )
            }
            is TravelOfferReservationCanceledEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null
                    )
                )
            }
            is TravelOfferBookedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                    )
                )
            }
            is TravelOfferReleaseEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.RELEASING,
                    )
                )
            }
            is TravelOfferBookingCanceledEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null
                    )
                )
            }
            is TravelOfferRebookedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                    )
                )
            }
            is TravelOfferExpiredEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.EXPIRED
                    )
                )
            }
            is TravelOfferMadeUnavailableEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.UNAVAILABLE
                    )
                )
            }
            is TravelOfferMadeAvailableEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdateStatus(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE
                    )
                )
            }
            is TravelOfferBookedCompensatedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.AVAILABLE,
                        bookingId = null
                    )
                )
            }
            is TravelOfferBookingCanceledCompensatedEvent -> {
                travelOfferQueryRepository.update(
                    TravelOfferUpdate(
                        _id = event.travelOfferId,
                        status = TravelOfferStatusEnum.BOOKED,
                        bookingId = event.bookingId,
                    )
                )
            }
        }
    }
}