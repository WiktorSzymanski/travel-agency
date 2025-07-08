package pl.szymanski.wiktor.ta.infrastructure.repository

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
        collection.find(org.bson.Document("commuteId", commuteId)).toList().firstOrNull()

    override suspend fun save(entity: Commute): Commute? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findAll(): List<Commute> = collection.find().toList()
}
