package pl.szymanski.wiktor.ta.infrastructure.projection

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToStreamOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteAvailableEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteBookingCanceledEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteExpiredEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteFullEvent
import pl.szymanski.wiktor.ta.event.CommuteBookedCompensatedEvent
import pl.szymanski.wiktor.ta.event.CommuteBookingCanceledCompensatedEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.command.commuteEventTypeRegistry
import pl.szymanski.wiktor.ta.queryRepository.CommuteCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateStatus

class CommuteProjectionService(
    private val kurrentDBClient: KurrentDBClient,
    private val commuteQueryRepository: CommuteQueryRepository
) {
    fun startProjection() {
        val streamName = "\$ce-commute"

        val subscriptionOptions = SubscribeToStreamOptions.get()
            .fromStart()
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val eventTypeName = resolvedEvent.event.eventType
                    val eventClass = commuteEventTypeRegistry[eventTypeName]
                        ?: throw IllegalArgumentException("Unknown event type: $eventTypeName")

                    updateProjection(EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass))
                }
            }
        }

        kurrentDBClient.subscribeToStream(streamName, listener, subscriptionOptions)
    }

    private suspend fun updateProjection(event: CommuteEvent) {
        when (event) {
            is CommuteCreatedEvent -> {
                commuteQueryRepository.save(
                    Commute(
                        _id = event.commuteId,
                        name = event.name,
                        departure = event.departure,
                        arrival = event.arrival,
                        seats = event.seats,
                        status = CommuteStatusEnum.SCHEDULED
                    )
                )
            }
            is CommuteBookedEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                    )
                )
            }
            is CommuteBookingCanceledEvent -> {
                commuteQueryRepository.update(
                    CommuteCancelUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                    )
                )
            }
            is CommuteExpiredEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.EXPIRED
                    )
                )
            }
            is CommuteFullEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.FULL
                    )
                )
            }
            is CommuteAvailableEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdateStatus(
                        _id = event.commuteId,
                        status = CommuteStatusEnum.SCHEDULED
                    )
                )
            }
            is CommuteBookedCompensatedEvent -> {
                commuteQueryRepository.update(
                    CommuteCancelUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                    )
                )
            }
            is CommuteBookingCanceledCompensatedEvent -> {
                commuteQueryRepository.update(
                    CommuteUpdate(
                        _id = event.commuteId,
                        bookingId = event.bookingId,
                    )
                )
            }
        }
    }
}