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
import io.kurrent.dbclient.*
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService

class EventRepositoryImpl : EventRepository {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferExpireService::class.java)
    }

    val map = mutableMapOf<String, Long>()

    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun save(event: Event, revision: Int) {
        val options = when (revision) {
            -1 -> AppendToStreamOptions.get().streamState(StreamState.noStream())
            else -> AppendToStreamOptions.get().streamRevision(revision.toLong())
        }

        val streamName = getStreamName(event)
        val eventData = prepareEventData(event)

        retryOnUnavailable {
            runCatching{ kurrentClient.appendToStream(streamName, options, eventData).await() }
                .onFailure {
                    if (it is WrongExpectedVersionException) throw ConcurrentModificationException("event - ${event}\nrevision - ${revision}\nmessage - ${it.message}")
                    else throw it
                }
        }
    }

    override suspend fun noRevisionSave(event: Event) {
        val streamName = getStreamName(event)
        val eventData = prepareEventData(event)

        retryOnUnavailable {
            kurrentClient.appendToStream(streamName, eventData).await()
        }
    }

    fun prepareEventData(event: Event): EventData {
        val serializedEvent = EventJsonSerializer.toBytes(event)
        val serializedMetadata = EventJsonSerializer.toBytes(mapOf("\$correlationId" to event.correlationId))

        val eventData = EventData.builderAsJson(event.eventId, event::class.java.name, serializedEvent)
            .metadataAsBytes(serializedMetadata)
            .build()

        return eventData
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

    override suspend fun subscribe(
        eventClass: Class<Event>,
        positionPair: Pair<Long, Long>,
        doOnEvent: suspend (Event) -> Unit
    ) {
        var checkpoint = positionPair.toPosition()

        val filter = SubscriptionFilter.newBuilder()
            .addEventTypePrefix(eventClass.simpleName)
            .build()

        val subscriptionOptions = SubscribeToAllOptions.get()
            .filter(filter)
            .fromPosition(checkpoint)
            .resolveLinkTos()

        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val event = EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass)
                    doOnEvent(event)
                    checkpoint = resolvedEvent.originalEvent.position
                }
            }

            override fun onCancelled(subscription: Subscription, exception: Throwable) {
                CoroutineScope(Dispatchers.Default).launch {
                    log.warn("Subscription for ${eventClass.simpleName} stream dropped: ${exception.message}")
                    subscribe(eventClass, checkpoint.toPair(), doOnEvent)
                }
            }
        }

        log.info("Subscribing to ${eventClass.simpleName} stream from position: $checkpoint")
        kurrentClient.subscribeToAll(listener, subscriptionOptions)
    }

    fun Pair<Long, Long>.toPosition() = Position(first, second)

    fun Position.toPair() = this.commitUnsigned to this.prepareUnsigned
}