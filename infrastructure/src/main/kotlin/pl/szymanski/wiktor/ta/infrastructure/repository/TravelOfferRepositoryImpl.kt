package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.MongoBulkWriteException
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferRepository {
    companion object {
        const val DUPLICATE_ERROR_CODE = 11000
    }

    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findById(travelOfferId: UUID): TravelOffer? =
        collection.find(org.bson.Document("_id", travelOfferId)).toList().firstOrNull()

    override suspend fun save(travelOffer: TravelOffer): TravelOffer? =
        collection.insertOne(travelOffer).insertedId?.let { travelOffer }

    override suspend fun findAll(): List<TravelOffer> = collection.find().toList()

    // db.travelOffer.createIndex( {commuteId: 1, attractionId: 1, accommodationId: 1 }, {unique: true} )
    override suspend fun saveAll(travelOffers: List<TravelOffer>) {
        try {
            collection.insertMany(travelOffers, InsertManyOptions().ordered(false))
        } catch (e: MongoBulkWriteException) {
            if (!e.writeErrors.all { it.code == DUPLICATE_ERROR_CODE }) throw e
        }
    }
}
