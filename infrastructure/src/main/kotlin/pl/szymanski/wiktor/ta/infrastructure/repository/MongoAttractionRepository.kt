package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider

class MongoAttractionRepository(
    private val databaseProvider: DatabaseProvider,
) : AttractionRepository {
    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<Attraction>("attractions")

    override suspend fun findById(id: AttractionId): Attraction {
        if (id !is AttractionId.Present) throw NoSuchElementException("Attraction not found: $id")

        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Attraction not found: $id")
    }

    override suspend fun create(entity: Attraction, event: AttractionEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Attraction, event: AttractionEvent) {
        val id = (entity.id as AttractionId.Present).value.toString()
        collection.replaceOne(
            Filters.eq("id", id),
            entity,
            ReplaceOptions().upsert(true)
        )
    }
}
