package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.kurrent.dbclient.EventData
import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import kotlinx.coroutines.future.await
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.EventJsonSerializer
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import java.util.*

// Projection model for Commute queries
data class CommuteProjection(
    val _id: UUID,
    val status: CommuteStatusEnum
)

// Event store model for storing events
data class CommuteEventRecord(
    val _id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CommuteRepositoryImpl(
    database: MongoDatabase,
) : CommuteRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val kurrentClient: KurrentDBClient = KurrentDbProvider.client

    override suspend fun findById(commuteId: UUID): Commute {
        val streamName = "commute-$commuteId"

        val options = ReadStreamOptions.get()
            .forwards()
            .fromStart()

        try {
            val readResult = retryOnUnavailable {
                kurrentClient.readStream(streamName, options).await()
            }

            val events: List<Pair<CommuteEvent, Int>> = readResult.events.map { resolvedEvent ->
                val eventTypeName = resolvedEvent.event.eventType
                val eventClass: Class<*> = Class.forName(eventTypeName)

                (EventJsonSerializer.fromBytes(resolvedEvent.event.eventData, eventClass) to resolvedEvent.event.revision.toInt()) as Pair<CommuteEvent, Int>
            }

            return Commute.fromEvents(events)
                ?: throw NoSuchElementException("Commute with ID $commuteId not found")
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.DEADLINE_EXCEEDED) {
                log.error("Call failed with ${e.status.code} for stream $streamName")
            }
            throw e
        }
    }

    override suspend fun save(event: Event) {
        if (event !is CommuteEvent) {
            throw IllegalArgumentException("Event must be a CommuteEvent")
        }
        
        try {
            val streamName = "commute-${event.commuteId}"
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
