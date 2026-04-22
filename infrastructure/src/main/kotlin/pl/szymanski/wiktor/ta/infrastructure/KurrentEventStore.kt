package pl.szymanski.wiktor.ta.infrastructure

import io.kurrent.dbclient.AppendToStreamOptions
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.StreamState
import io.kurrent.dbclient.SubscribeToAllOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionFilter
import io.kurrent.dbclient.SubscriptionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.event.DateMetEvent
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.eventstore.EventStore
import pl.szymanski.wiktor.ta.infrastructure.outbox.OutboxRepositoryMongo
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.outbox.OutboxEntry
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@Service
class KurrentEventStore(
    private val kurrentClient: KurrentDBClient,
    private val objectMapper: ObjectMapper,
    private val outboxRepository: OutboxRepositoryMongo,
) : EventStore {
    override suspend fun publish(eventEnvelope: EventEnvelope<out PublishableEvent>) {

        val aggregateId = eventEnvelope.event.entityId.toString()
        val topic = topicResolver(eventEnvelope.event)
        val streamName = "$topic-$aggregateId"

        val eventId = eventEnvelope.event.eventId
        val eventType = eventEnvelope.eventType
        val dataBytes = objectMapper.writeValueAsBytes(eventEnvelope)
        val metaBytes = objectMapper.writeValueAsBytes(eventEnvelope.metadata)
        val builder = EventData.builderAsJson(eventType, dataBytes)
        builder.eventId(eventId)
        builder.metadataAsBytes(metaBytes)
        val eventData = builder.build()

        val streamState = if (eventEnvelope.metadata.revision <= 0L) {
            StreamState.noStream()
        } else {
            StreamState.streamRevision(eventEnvelope.metadata.revision - 1)
        }

        kurrentClient.appendToStream(
            streamName,
            AppendToStreamOptions.get().streamState(streamState),
            eventData
        ).await()
    }

    override suspend fun publishAt(eventEnvelope: EventEnvelope<out PublishableEvent>, processAfter: LocalDateTime) {
        outboxRepository.save(OutboxEntry(eventEnvelope, processAfter))
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

    private fun topicResolver(event: PublishableEvent): String = when (event) {
        is AccommodationEvent -> "accommodation"
        is AttractionEvent -> "attraction"
        is CommuteEvent -> "commute"
        is BookingEvent -> "booking"
        is SagaEvent -> "saga"
        is DateMetEvent -> "date-met"
        else -> throw IllegalArgumentException("No topic defined for event type: ${event::class.simpleName}")
    }

}