package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import java.util.ConcurrentModificationException
import java.util.UUID

class AttractionRepositoryImpl(
    database: MongoDatabase,
) : AttractionRepository {
    private val collection: MongoCollection<Attraction> = database.getCollection("attraction")

    override suspend fun findById(attractionId: UUID): Attraction = collection.find(Document("_id", attractionId)).toList().first()

    override suspend fun save(entity: Attraction): Attraction? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun update(entity: Attraction) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("version", entity.version),
        )
        val update =
            Updates.combine(
                Updates.set("bookings", entity.bookings),
                Updates.set("status", "${entity.status}"),
                Updates.set("version", entity.version + 1),
            )
        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Concurrent modification detected for ${entity._id}")
        }
    }

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> =
        collection.find(Document("status", status.toString())).toList()


    // All used by command side
}
