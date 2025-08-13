package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToStreamOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AccommodationExpiredEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.AccommodationBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.accommodationEventTypeRegistry
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdate
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateStatus

class AccommodationProjectionService(
    private val kurrentDBClient: KurrentDBClient,
    private val accommodationQueryRepository: AccommodationQueryRepository
) {
    fun startProjection() {
        val streamName = "\$ce-accommodation"

        val subscriptionOptions = SubscribeToStreamOptions.get()
            .fromStart()
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val eventTypeName = resolvedEvent.event.eventType
                    val eventClass = accommodationEventTypeRegistry[eventTypeName]
                        ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

                    updateProjection(EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass))
                }
            }
        }

        kurrentDBClient.subscribeToStream(streamName, listener, subscriptionOptions)
    }

    private suspend fun updateProjection(event: AccommodationEvent) {
        when (event) {
            is AccommodationCreatedEvent -> {
                accommodationQueryRepository.save(
                    Accommodation(
                        _id = event.accommodationId,
                        name = event.name,
                        location = event.location,
                        rent = event.rent,
                        status = AccommodationStatusEnum.AVAILABLE
                    )
                )
            }
            is AccommodationBookedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.BOOKED,
                        bookingId = event.bookingId
                    )
                )
            }
            is AccommodationBookingCanceledEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.AVAILABLE,
                        bookingId = null
                    )
                )
            }
            is AccommodationExpiredEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdateStatus(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.EXPIRED,
                    )
                )
            }
            is AccommodationBookedCompensatedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.AVAILABLE,
                        bookingId = null
                    )
                )
            }
            is AccommodationBookingCanceledCompensatedEvent -> {
                accommodationQueryRepository.update(
                    AccommodationUpdate(
                        _id = event.accommodationId,
                        status = AccommodationStatusEnum.BOOKED,
                        bookingId = event.bookingId
                    )
                )
            }
        }
    }
}