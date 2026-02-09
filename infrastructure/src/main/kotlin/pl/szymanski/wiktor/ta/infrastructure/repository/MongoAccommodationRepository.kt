package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider

class MongoAccommodationRepository(
    private val databaseProvider: DatabaseProvider,
) : AccommodationRepository {
    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<Accommodation>("accommodations")

    override suspend fun findById(id: AccommodationId): Accommodation {
        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Accommodation not found: $id")
    }

    override suspend fun create(entity: Accommodation, event: AccommodationEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Accommodation, event: AccommodationEvent) {
        collection.replaceOne(
            Filters.eq("id", entity.id.value.toString()),
            entity,
            ReplaceOptions().upsert(true)
        )
    }
}
