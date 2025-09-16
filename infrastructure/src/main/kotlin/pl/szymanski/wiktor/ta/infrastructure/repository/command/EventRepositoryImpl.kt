package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.CosmosAsyncContainer
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

    override suspend fun save(event: Event, etag: String) {
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

        container.createItem(
            persistedEvent,
            PartitionKey(stream),
            CosmosItemRequestOptions().setIfMatchETag(etag)
        ).awaitSingle()
    }

    override suspend fun noRevisionSave(event: Event) {
        val stream = getStreamName(event)

        val persistedEvent = PersistedEvent(
            id = event.eventId,
            stream = stream,
            type = event::class.java.name,
            correlationid = event.correlationId!!,
            timestamp = LocalDateTime.now().toString(),
            revision = -2,
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