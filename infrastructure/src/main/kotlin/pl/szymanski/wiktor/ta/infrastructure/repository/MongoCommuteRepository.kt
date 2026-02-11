package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.flow.firstOrNull
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
    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<Commute>("commutes")

    override suspend fun findById(id: CommuteId): Commute {
        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Commute not found: $id")
    }

    override suspend fun create(entity: Commute, event: CommuteEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Commute, event: CommuteEvent) {
        collection.replaceOne(
            Filters.eq("id", entity.id.value.toString()),
            entity,
            ReplaceOptions().upsert(true)
        )
    }
}
