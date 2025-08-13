package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToStreamOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.event.AttractionAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionFullEvent
import pl.szymanski.wiktor.ta.event.AttractionBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AttractionBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.attractionEventTypeRegistry
import pl.szymanski.wiktor.ta.queryRepository.AttractionCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateStatus

class AttractionProjectionService(
    private val kurrentDBClient: KurrentDBClient,
    private val attractionQueryRepository: AttractionQueryRepository
) {
    fun startProjection() {
        val streamName = "\$ce-attraction"

        val subscriptionOptions = SubscribeToStreamOptions.get()
            .fromStart()
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val eventTypeName = resolvedEvent.event.eventType
                    val eventClass = attractionEventTypeRegistry[eventTypeName]
                        ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

                    updateProjection(EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass))
                }
            }
        }

        kurrentDBClient.subscribeToStream(streamName, listener, subscriptionOptions)
    }

    private suspend fun updateProjection(event: AttractionEvent) {
        when (event) {
            is AttractionCreatedEvent -> {
                attractionQueryRepository.save(
                    Attraction(
                        _id = event.attractionId,
                        name = event.name,
                        location = event.location,
                        date = event.date,
                        capacity = event.capacity,
                        status = AttractionStatusEnum.SCHEDULED
                    )
                )
            }
            is AttractionBookedEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdate(
                        _id = event.attractionId,
                        bookingId = event.bookingId
                    )
                )
            }
            is AttractionBookingCanceledEvent -> {
                attractionQueryRepository.update(
                    AttractionCancelUpdate(
                        _id = event.attractionId,
                        bookingId = event.bookingId,
                    )
                )
            }
            is AttractionExpiredEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        _id = event.attractionId,
                        status = AttractionStatusEnum.EXPIRED,
                    )
                )
            }
            is AttractionFullEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        _id = event.attractionId,
                        status = AttractionStatusEnum.FULL
                    )
                )
            }
            is AttractionAvailableEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdateStatus(
                        _id = event.attractionId,
                        status = AttractionStatusEnum.SCHEDULED
                    )
                )
            }
            is AttractionBookedCompensatedEvent -> {
                attractionQueryRepository.update(
                    AttractionCancelUpdate(
                        _id = event.attractionId,
                        bookingId = event.bookingId,
                    )
                )
            }
            is AttractionBookingCanceledCompensatedEvent -> {
                attractionQueryRepository.update(
                    AttractionUpdate(
                        _id = event.attractionId,
                        bookingId = event.bookingId,
                    )
                )
            }
        }
    }
}