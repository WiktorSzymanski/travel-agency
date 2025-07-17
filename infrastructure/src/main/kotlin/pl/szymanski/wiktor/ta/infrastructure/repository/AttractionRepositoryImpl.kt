package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import java.util.UUID

class AttractionRepositoryImpl(
    database: MongoDatabase,
) : AttractionRepository {
    private val collection: MongoCollection<Attraction> = database.getCollection("attraction")

    override suspend fun findById(attractionId: UUID): Attraction = collection.find(org.bson.Document("_id", attractionId)).toList().first()

    override suspend fun save(entity: Attraction): Attraction? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun update(entity: Attraction) {
        val filter = org.bson.Document("_id", entity._id)
        val update =
            Updates.combine(
                Updates.set("bookings", entity.bookings),
                Updates.set("status", "${entity.status}"),
            )

        collection.updateOne(filter, update)
    }

    override suspend fun findAll(): List<Attraction> = collection.find().toList()

    override suspend fun updateAllStatus(attractions: List<Attraction>) {
        collection.bulkWrite(
            attractions.map { attraction ->
                UpdateOneModel(
                    Filters.eq("_id", attraction._id),
                    Updates.set("status", "${attraction.status}"),
                )
            },
        )
    }
}
