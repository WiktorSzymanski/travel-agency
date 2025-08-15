package pl.szymanski.wiktor.ta.infrastructure.repository.command

import io.kurrent.dbclient.AppendToStreamOptions
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ResolvedEvent
import io.kurrent.dbclient.StreamState
import io.kurrent.dbclient.SubscribeToAllOptions
import io.kurrent.dbclient.Subscription
import io.kurrent.dbclient.SubscriptionFilter
import io.kurrent.dbclient.SubscriptionListener
import io.kurrent.dbclient.WrongExpectedVersionException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.EventRepository
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.UUID

class EventRepositoryImpl() : EventRepository {
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun save(event: Event, revision: Int) {
        val options = when (revision) {
            -1 -> AppendToStreamOptions.get().streamState(StreamState.noStream())
            else -> AppendToStreamOptions.get().streamRevision(revision.toLong())
        }

        val streamName = when (event) {
            is CommuteEvent -> "commute-${event.commuteId}"
            is AccommodationEvent -> "accommodation-${event.accommodationId}"
            is AttractionEvent -> "attraction-${event.attractionId}"
            is TravelOfferEvent -> "travelOffer-${event.travelOfferId}"
            is BookingEvent -> "booking-${event.bookingId}"
            is SagaEvent -> "saga-${event.correlationId}"
            else -> { throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}") }
        }

        val serializedEvent = EventJsonSerializer.toBytes(event)
        val serializedMetadata = EventJsonSerializer.toBytes(mapOf("\$correlationId" to event.correlationId))

        val eventData = EventData.builderAsJson(event.eventId, event::class.simpleName, serializedEvent)
            .metadataAsBytes(serializedMetadata)
            .build()

        runCatching{ kurrentClient.appendToStream(streamName, options, eventData).await() }
            .onFailure { if (it is WrongExpectedVersionException) throw ConcurrentModificationException(it.message) }
    }

    override suspend fun noRevisionSave(event: Event) {
        val streamName = when (event) {
            is CommuteEvent -> "commute-${event.commuteId}"
            is AccommodationEvent -> "accommodation-${event.accommodationId}"
            is AttractionEvent -> "attraction-${event.attractionId}"
            is TravelOfferEvent -> "travelOffer-${event.travelOfferId}"
            is BookingEvent -> "booking-${event.bookingId}"
            is SagaEvent -> "saga-${event.correlationId}"
            else -> { throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}") }
        }

        val serializedEvent = EventJsonSerializer.toBytes(event)
        val serializedMetadata = EventJsonSerializer.toBytes(mapOf("\$correlationId" to event.correlationId))

        val eventData = EventData.builderAsJson(event.eventId, event::class.simpleName, serializedEvent)
            .metadataAsBytes(serializedMetadata)
            .build()

        kurrentClient.appendToStream(streamName, eventData)
    }

    override suspend fun subscribe(eventClass: Class<Event>, onEvent: suspend (Event) -> Unit) {
        val scope = CoroutineScope(Dispatchers.Default)

        val filter = SubscriptionFilter.newBuilder()
            .addEventTypePrefix(eventClass.simpleName)
            .build()

        val subscriptionOptions = SubscribeToAllOptions.get()
            .filter(filter)
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                scope.launch {
                    val event = EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass)
                    onEvent(event)
                }
            }
        }

        kurrentClient.subscribeToAll(listener, subscriptionOptions)
    }
}