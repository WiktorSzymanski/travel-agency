package pl.szymanski.wiktor.ta.infrastructure

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.SubscribeToAllOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionFilter
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.eventstore.EventStore
import pl.szymanski.wiktor.ta.infrastructure.outbox.OutboxRepositoryMongo
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import java.time.LocalDateTime
import java.util.UUID

class KurrentEventStore(
    private val kurrentClient: KurrentDBClient,
    private val outboxRepository: OutboxRepositoryMongo,
) : EventStore {
    override suspend fun append(event: EventEnvelope<out PublishableEvent>) {
        val eventData = EventJsonSerializer.toEventData(event)
        kurrentClient.appendToStream(event.eventType, eventData).await()
    }

    override suspend fun appendAt(event: EventEnvelope<out PublishableEvent>, processAfter: LocalDateTime) {
        outboxRepository.save(OutboxEntry(event, processAfter))
    }

    override suspend fun subscribe(
        eventType: Class<out PublishableEvent>,
        handler: suspend (EventEnvelope<out PublishableEvent>) -> Unit
    ) {
        val filter = SubscriptionFilter.newBuilder().addEventTypePrefix(eventType.simpleName).build()
        val options = SubscribeToAllOptions.get().filter(filter).fromStart().resolveLinkTos()
        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val envelope = EventJsonSerializer.fromResolvedEvent(resolvedEvent, eventType)
                    handler(envelope)
                }
            }
        }
        kurrentClient.subscribeToAll(listener, options)
    }

    suspend fun getPendingEntries(limit: Int): List<OutboxEntry> {
        return outboxRepository.getPendingEntries(limit)
    }

    suspend fun markAsPublished(eventId: UUID) {
        outboxRepository.markAsPublished(eventId)
    }

    fun getStreamName(event: Event): String = when (event) {
        is CommuteEvent -> "commute-${event.commuteId}"
        is AccommodationEvent -> "accommodation-${event.accommodationId}"
        is AttractionEvent -> "attraction-${event.attractionId}"
        is TravelOfferEvent -> "travelOffer-${event.travelOfferId}"
        is BookingEvent -> "booking-${event.bookingId}"
        is SagaEvent -> "saga-${event.correlationId}"
        else -> { throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}") }
    }

}