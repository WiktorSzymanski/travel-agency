package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.ReadStreamOptions
import tools.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AggregateId
import pl.szymanski.wiktor.ta.domain.event.PublishableEvent
import pl.szymanski.wiktor.ta.repository.CommandRepository
import kotlin.reflect.KClass

abstract class KurrentCommandRepository<T : Any, R : Any, E : PublishableEvent>(
    protected val client: KurrentDBClient,
    protected val objectMapper: ObjectMapper,
    private val aggregateClass: KClass<T>,
    private val eventClass: KClass<E>,
    private val fromEventsFunc: (List<E>) -> T
) : CommandRepository<T, R> {

    protected open val aggregateType: String = aggregateClass.simpleName!!

    override suspend fun create(entity: T, metadata: Metadata) {
        // This is now handled by OutboxPort.save which appends events
        // In some ES designs, repository.save would do it. 
        // But the current flow uses outboxPort.save.
    }

    override suspend fun save(entity: T, metadata: Metadata) {
        // Also handled by OutboxPort.save
    }

    override fun createBlocking(entity: T, metadata: Metadata) {
        // Not used anymore in the main flow
    }

    override fun saveBlocking(entity: T, metadata: Metadata) {
        // Not used anymore in the main flow
    }

    override suspend fun findById(id: R): Pair<T, Long> {
        val aggregateId = (id as? AggregateId)?.value?.toString() ?: id.toString()
        val streamName = "$aggregateType-$aggregateId"
        
        return try {
            val result = client.readStream(streamName, ReadStreamOptions.get()).await()
            val events = result.events.map { resolvedEvent ->
                objectMapper.readValue(resolvedEvent.event.eventData as ByteArray, eventClass.java)
            }
            
            if (events.isEmpty()) throw NoSuchElementException("$aggregateType not found: $id")
            
            val aggregate = fromEventsFunc(events)
            val version = result.events.last().originalEvent.revision
            
            aggregate to version
        } catch (e: Exception) {
             throw NoSuchElementException(
                 "$aggregateType not found for stream '$streamName'. Cause: ${e::class.simpleName}: ${e.message ?: "<no message>"}"
             )
        }
    }
}
