package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import java.util.UUID

class CommuteRepositoryImpl(
    database: MongoDatabase,
) : CommuteRepository {
    private val collection: MongoCollection<Commute> = database.getCollection("commute")

    override suspend fun findById(commuteId: UUID): Commute? =
        collection.find(org.bson.Document("_id", commuteId)).toList().firstOrNull()

    override suspend fun save(entity: Commute): Commute? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findAll(): List<Commute> = collection.find().toList()

    override suspend fun updateAllStatus(commutes: List<Commute>) {
        collection.bulkWrite(
            commutes.map { commute ->
                UpdateOneModel(
                    Filters.eq("_id", commute._id),
                    Updates.set("status", "${commute.status}"),
                )
            },
        )
    }
}
