package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import java.util.UUID

class AccommodationRepositoryImpl(
    database: MongoDatabase,
) : AccommodationRepository {
    private val collection: MongoCollection<Accommodation> = database.getCollection("accommodation")

    override suspend fun findById(accommodationId: UUID): Accommodation? =
        collection.find(org.bson.Document("accommodationId", accommodationId)).toList().firstOrNull()

    override suspend fun save(entity: Accommodation): Accommodation? =
        collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findAll(): List<Accommodation> = collection.find().toList()
}
