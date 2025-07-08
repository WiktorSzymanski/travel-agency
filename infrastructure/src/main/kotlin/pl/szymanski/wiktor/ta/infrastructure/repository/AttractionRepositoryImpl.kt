package pl.szymanski.wiktor.ta.infrastructure.repository

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

    override suspend fun findById(attractionId: UUID): Attraction? =
        collection.find(org.bson.Document("attractionId", attractionId)).toList().firstOrNull()

    override suspend fun save(entity: Attraction): Attraction? =
        collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findAll(): List<Attraction> = collection.find().toList()
}
