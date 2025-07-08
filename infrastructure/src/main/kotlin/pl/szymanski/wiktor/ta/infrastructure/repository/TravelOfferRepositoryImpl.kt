package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferRepository {
    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findById(travelOfferId: UUID): TravelOffer? =
        collection.find(org.bson.Document("travelOfferId", travelOfferId)).toList().firstOrNull()

    override suspend fun save(travelOffer: TravelOffer): TravelOffer? =
        collection.insertOne(travelOffer).insertedId?.let { travelOffer }

    override suspend fun findAll(): List<TravelOffer> = collection.find().toList()
}
