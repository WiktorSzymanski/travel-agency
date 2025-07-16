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

    override suspend fun findById(accommodationId: UUID): Accommodation =
        collection.find(org.bson.Document("_id", accommodationId)).toList().first()

    override suspend fun save(entity: Accommodation): Accommodation? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun update(entity: Accommodation) {
        val filter = org.bson.Document("_id", entity._id)
        val update = Updates.combine(
            Updates.set("booking", entity.booking),
            Updates.set("status", "${entity.status}")
        )

        collection.updateOne(filter, update)
    }

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
