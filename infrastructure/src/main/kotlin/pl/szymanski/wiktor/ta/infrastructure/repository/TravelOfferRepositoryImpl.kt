package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID

class TravelOfferRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferRepository {
    // db.travelOffer.createIndex( {commuteId: 1, attractionId: 1, accommodationId: 1 }, {unique: true} )
    companion object {
        const val DUPLICATE_ERROR_CODE = 11000
    }

    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findById(travelOfferId: UUID): TravelOffer =
        collection.find(org.bson.Document("_id", travelOfferId)).toList().first()

    override suspend fun save(travelOffer: TravelOffer): TravelOffer? = collection.insertOne(travelOffer).insertedId?.let { travelOffer }

    override suspend fun update(travelOffer: TravelOffer) {
        val filter = org.bson.Document("_id", travelOffer._id)
        val update =
            Updates.combine(
                Updates.set("booking", travelOffer.booking),
                Updates.set("status", "${travelOffer.status}"),
            )

        collection.updateOne(filter, update)
    }

    override suspend fun findAll(): List<TravelOffer> = collection.find().toList()

    override suspend fun findByStatus(status: TravelOfferStatusEnum): List<TravelOffer> =
        collection.find(org.bson.Document("status", status)).toList()

    override suspend fun findByCommuteId(commuteId: UUID): List<TravelOffer> =
        collection.find(org.bson.Document("commuteId", commuteId)).toList()

    override suspend fun findByAccommodationId(accommodationId: UUID): List<TravelOffer> =
        collection.find(org.bson.Document("accommodationId", accommodationId)).toList()

    override suspend fun findByAttractionId(attractionId: UUID): List<TravelOffer> =
        collection.find(org.bson.Document("attractionId", attractionId)).toList()
}
