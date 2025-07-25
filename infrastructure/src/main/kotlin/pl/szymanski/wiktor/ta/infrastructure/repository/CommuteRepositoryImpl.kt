package pl.szymanski.wiktor.ta.infrastructure.repository

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
        val filter = Document("_id", entity._id)
        val update =
            Updates.combine(
                Updates.set("bookings", entity.bookings),
                Updates.set("status", "${entity.status}"),
            )
        collection.updateOne(filter, update)
    }

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> =
        collection.find(Document("status", status.toString())).toList()
}
