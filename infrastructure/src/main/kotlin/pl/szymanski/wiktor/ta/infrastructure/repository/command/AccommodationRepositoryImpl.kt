package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import java.util.*

class AccommodationRepositoryImpl(
    database: MongoDatabase,
) : AccommodationRepository {
    private val collection: MongoCollection<Accommodation> = database.getCollection("accommodation")

    override suspend fun findById(accommodationId: UUID): Accommodation =
        collection.find(Document("_id", accommodationId)).toList().first()

    override suspend fun save(entity: Accommodation): Accommodation? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun update(entity: Accommodation) {
        val filter = Document("_id", entity._id)
        val update =
            Updates.combine(
                Updates.set("booking", entity.booking),
                Updates.set("status", "${entity.status}"),
            )

        collection.updateOne(filter, update)
    }

    override suspend fun findAllByStatus(status: AccommodationStatusEnum): List<Accommodation> =
        collection.find(Document("status", status.toString())).toList()
}
