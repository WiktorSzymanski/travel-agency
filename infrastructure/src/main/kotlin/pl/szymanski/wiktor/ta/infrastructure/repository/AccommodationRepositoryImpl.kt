package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import java.util.UUID

class AccommodationRepositoryImpl(
    database: MongoDatabase,
) : AccommodationRepository {
    private val collection: MongoCollection<Accommodation> = database.getCollection("accommodation")

    override suspend fun findById(accommodationId: UUID): Accommodation? =
        collection.find(org.bson.Document("_id", accommodationId)).toList().firstOrNull()

    override suspend fun save(entity: Accommodation): Accommodation? =
        collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findAll(): List<Accommodation> = collection.find().toList()

    override suspend fun updateAllStatus(accommodations: List<Accommodation>) {
        collection.bulkWrite(
            accommodations.map { accommodation ->
                UpdateOneModel(
                    Filters.eq("_id", accommodation._id),
                    Updates.set("status", "${accommodation.status}"),
                )
            },
        )
    }
}
