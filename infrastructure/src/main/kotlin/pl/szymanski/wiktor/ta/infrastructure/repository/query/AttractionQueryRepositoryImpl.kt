package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.queryRepository.AttractionCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateStatus
import java.util.UUID

class AttractionQueryRepositoryImpl(
    database: MongoDatabase,
) : AttractionQueryRepository {
    private val collection: MongoCollection<Attraction> = database.getCollection("attraction")

    override suspend fun save(entity: Attraction): Attraction? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(attractionId: UUID): Attraction = collection.find(Document("_id", attractionId)).toList().first()

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: AttractionUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )

        val update = Updates.addToSet("bookings", entity.bookingId)

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun update(entity: AttractionCancelUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )

        val update = Updates.pull("bookings", entity.bookingId)

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun update(entity: AttractionUpdateStatus) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )
        val update = Updates.combine(
            Updates.set("status", "${entity.status}"),
        )

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }
}