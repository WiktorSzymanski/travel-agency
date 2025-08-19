package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.queryRepository.AttractionCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateStatus
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class AttractionQueryRepositoryImpl(
    database: MongoDatabase,
) : AttractionQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    val maxRetries = 10
    val initialDelayMs = 100L
    val maxDelayMs = 10000L
    val jitterFactor = 0.1

    private val collection: MongoCollection<Attraction> = database.getCollection("attraction")

    override suspend fun save(entity: Attraction): Attraction? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(attractionId: UUID): Attraction = collection.find(Document("_id", attractionId)).firstOrNull() ?: throw NoSuchElementException()

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: AttractionUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.revision - 1)
        )

        val update = Updates.combine(
            Updates.addToSet("bookings", entity.bookingId),
            Updates.set("lastRevision", entity.revision)
        )

        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
        }
    }

    override suspend fun update(entity: AttractionUpdateRevision) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.revision - 1)
        )

        val update = Updates.combine(
            Updates.addToSet("bookings", entity.bookingId),
            Updates.set("lastRevision", entity.revision)
        )

        runCatching {
            withRetry(
                maxRetries = maxRetries,
                initialDelayMs = initialDelayMs,
                maxDelayMs = maxDelayMs,
                jitterFactor = jitterFactor
            ) {
                if (collection.updateOne(filter, update).matchedCount == 0L) {
                    throw ConcurrentModificationException("Could not update ${entity}")
                }
            }
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }

    override suspend fun update(entity: AttractionCancelUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.revision - 1)
        )

        val update = Updates.combine(
            Updates.pull("bookings", entity.bookingId),
            Updates.set("lastRevision", entity.revision)
        )

        runCatching {
        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
        }
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }

    override suspend fun update(entity: AttractionUpdateStatus) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.revision - 1)
        )

        val update = Updates.combine(
            Updates.set("status", "${entity.status}"),
            Updates.set("lastRevision", entity.revision)
        )

        runCatching {
        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
        }
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }
}