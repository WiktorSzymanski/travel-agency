package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider

class MongoCommuteRepository(
    private val databaseProvider: DatabaseProvider,
) : CommuteRepository {
    @Serializable
    private data class CommuteEventDocument(
        val aggregateId: String,
        val event: CommuteEvent,
    )

    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<CommuteEventDocument>("commute_events")

    override suspend fun findById(id: CommuteId): Commute {
        val docs = collection
            .find(Filters.eq("aggregateId", id.value.toString()))
            .sort(Sorts.ascending("_id"))
            .toList()

        val events = docs.map { it.event }
        if (events.isEmpty()) throw NoSuchElementException("Commute not found: $id")
        return Commute.fromEvents(events)
    }

    override suspend fun create(entity: Commute, event: CommuteEvent) {
        collection.insertOne(
            CommuteEventDocument(
                aggregateId = entity.id.value.toString(),
                event = event,
            )
        )
    }

    override suspend fun save(entity: Commute, event: CommuteEvent) {
        collection.insertOne(
            CommuteEventDocument(
                aggregateId = entity.id.value.toString(),
                event = event,
            )
        )
    }
}
