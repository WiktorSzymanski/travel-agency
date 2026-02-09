package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider

class MongoAttractionRepository(
    private val databaseProvider: DatabaseProvider,
) : AttractionRepository {
    @Serializable
    private data class AttractionEventDocument(
        val aggregateId: String,
        val event: AttractionEvent,
    )

    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<AttractionEventDocument>("attraction_events")

    override suspend fun findById(id: AttractionId): Attraction {
        // AttractionId is a sealed interface; events always carry a Present ID
        val aggregateId = when (id) {
            is pl.szymanski.wiktor.ta.domain.aggregate.AttractionId.Present -> id.value.toString()
            pl.szymanski.wiktor.ta.domain.aggregate.AttractionId.Empty -> throw NoSuchElementException("Attraction not found: $id")
        }
        val docs = collection
            .find(Filters.eq("aggregateId", aggregateId))
            .sort(Sorts.ascending("_id"))
            .toList()

        val events = docs.map { it.event }
        if (events.isEmpty()) throw NoSuchElementException("Attraction not found: $id")
        return Attraction.fromEvents(events)
    }

    override suspend fun create(entity: Attraction, event: AttractionEvent) {
        val aggregateId = (entity.id as pl.szymanski.wiktor.ta.domain.aggregate.AttractionId.Present).value.toString()
        collection.insertOne(
            AttractionEventDocument(
                aggregateId = aggregateId,
                event = event,
            )
        )
    }

    override suspend fun save(entity: Attraction, event: AttractionEvent) {
        val aggregateId = (entity.id as pl.szymanski.wiktor.ta.domain.aggregate.AttractionId.Present).value.toString()
        collection.insertOne(
            AttractionEventDocument(
                aggregateId = aggregateId,
                event = event,
            )
        )
    }
}
