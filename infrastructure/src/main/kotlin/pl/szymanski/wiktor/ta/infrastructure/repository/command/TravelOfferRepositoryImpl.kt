package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import kotlinx.coroutines.future.await
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.*

// Projection model for TravelOffer queries
data class TravelOfferProjection(
    val _id: UUID,
    val commuteId: UUID,
    val accommodationId: UUID,
    val attractionId: UUID?
)

// Event store model for storing events
data class EventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TravelOfferRepositoryImpl(
    database: MongoDatabase
) : TravelOfferRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(travelOfferId: UUID): TravelOffer {
        val streamName = "travelOffer-$travelOfferId"

        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        try {
            val readResult = retryOnUnavailable {
                kurrentClient.readStream(streamName, options).await()
            }

            val events: List<Pair<TravelOfferEvent, Int>> = readResult.events.map { resolvedEvent ->
                val eventTypeName = resolvedEvent.event.eventType
                val eventClass: Class<*> = Class.forName(eventTypeName)

                (EventJsonSerializer.fromBytes(
                    resolvedEvent.event.eventData,
                    eventClass
                ) to resolvedEvent.event.revision.toInt()) as Pair<TravelOfferEvent, Int>
            }

            return TravelOffer.fromEvents(events)
                ?: throw NoSuchElementException("TravelOffer with ID $travelOfferId not found")
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
                log.error("Call failed with ${e.status.code} for stream $streamName")
            }
            throw e
        }
    }

    override suspend fun save(event: TravelOfferEvent) {
        try {
            val streamName = "travelOffer-${event.travelOfferId}"
            val serializedEvent = EventJsonSerializer.toBytes(event)
            val eventData = EventData.builderAsJson(event::class.simpleName, serializedEvent).build()
            retryOnUnavailable {
                kurrentClient.appendToStream(streamName, eventData)
            }
        } catch (e: Exception) {
            println("Failed to save event to KurrentDb: ${e.message}")
            throw e
        }
    }
}
