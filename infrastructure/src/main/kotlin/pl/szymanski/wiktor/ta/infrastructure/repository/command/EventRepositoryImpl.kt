package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.CosmosBatch
import com.azure.cosmos.models.CosmosBatchItemRequestOptions
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.PartitionKey
import io.kurrent.dbclient.*
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.EventRepository
import pl.szymanski.wiktor.ta.event.SagaEvent
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.scheduler.PersistedEvent
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService
import java.time.LocalDateTime

class EventRepositoryImpl(
    private val container: CosmosAsyncContainer
) : EventRepository {
    companion object {
        private val log = LoggerFactory.getLogger(TravelOfferExpireService::class.java)
    }

    override suspend fun save(event: Event, lastRevision: Long, lastEtag: String) {
        val stream = getStreamName(event)

        val persistedEvent = PersistedEvent(
            id = event.eventId,
            stream = stream,
            type = event::class.java.name,
            correlationid = event.correlationId!!,
            timestamp = LocalDateTime.now().toString(),
            revision = lastRevision + 1,
            domainevent = EventJsonSerializer.toJSON(event),
        )

        container.createItem(
            persistedEvent,
            PartitionKey(stream),
            CosmosItemRequestOptions().setIfMatchETag(lastEtag)
        ).awaitSingle()
    }

    override suspend fun save(events: List<Event>, lastRevision: Long, lastEtag: String) {
        val stream = getStreamName(events[0])
        val batch = CosmosBatch.createCosmosBatch(PartitionKey(stream))

        val persistedEvents = events.mapIndexed { idx, event ->
            PersistedEvent(
                id = event.eventId,
                stream = stream,
                type = event::class.java.name,
                correlationid = event.correlationId!!,
                timestamp = LocalDateTime.now().toString(),
                revision = lastRevision + 1 + idx,
                domainevent = EventJsonSerializer.toJSON(event),
            )
        }

        val first = persistedEvents.first()
        val rest = persistedEvents.drop(1)

        batch.createItemOperation(
            first,
            CosmosBatchItemRequestOptions().setIfMatchETag(lastEtag))

        rest.forEach { event ->
            batch.createItemOperation(event)
        }

        container.executeCosmosBatch(batch).awaitSingle()
    }

    override suspend fun noRevisionSave(event: Event) {
        val stream = getStreamName(event)

        val persistedEvent = PersistedEvent(
            id = event.eventId,
            stream = stream,
            type = event::class.java.name,
            correlationid = event.correlationId!!,
            timestamp = LocalDateTime.now().toString(),
            revision = -1,
            domainevent = EventJsonSerializer.toJSON(event),
        )
        container
            .createItem(persistedEvent)
            .awaitSingle()
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
    ) {}

    fun Pair<Long, Long>.toPosition() = Position(first, second)

    fun Position.toPair() = this.commitUnsigned to this.prepareUnsigned
}