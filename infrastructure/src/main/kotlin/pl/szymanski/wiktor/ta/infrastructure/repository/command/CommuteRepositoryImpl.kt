package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import java.util.*

class CommuteRepositoryImpl(
    database: MongoDatabase,
) : CommuteRepository {
    private val collection: MongoCollection<Commute> = database.getCollection("commute")

    override suspend fun findById(commuteId: UUID): Commute = collection.find(Document("_id", commuteId)).toList().first()

    override suspend fun save(entity: Commute): Commute? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun update(entity: Commute) {
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

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> =
        collection.find(Document("status", status.toString())).toList()
}
