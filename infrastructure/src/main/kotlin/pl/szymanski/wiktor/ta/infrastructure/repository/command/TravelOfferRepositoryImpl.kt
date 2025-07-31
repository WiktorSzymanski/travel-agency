package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.*
import kotlin.ConcurrentModificationException

class TravelOfferRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferRepository {
    // db.travelOffer.createIndex( {commuteId: 1, attractionId: 1, accommodationId: 1 }, {unique: true} )
    companion object {
        const val DUPLICATE_ERROR_CODE = 11000
    }

    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findById(travelOfferId: UUID): TravelOffer =
        collection.find(Document("_id", travelOfferId)).toList().first()

    override suspend fun save(travelOffer: TravelOffer): TravelOffer? = collection.insertOne(travelOffer).insertedId?.let { travelOffer }

    override suspend fun update(travelOffer: TravelOffer) {
        val filter = Filters.and(
            Filters.eq("_id", travelOffer._id),
            Filters.eq("version", travelOffer.version),
        )
        val update =
            Updates.combine(
                Updates.set("booking", travelOffer.booking),
                Updates.set("status", "${travelOffer.status}"),
                Updates.set("version", travelOffer.version + 1),
            )
        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Concurrent modification detected for ${travelOffer._id}")
        }
    }

    override suspend fun findByCommuteId(commuteId: UUID): List<TravelOffer> =
        collection.find(Document("commuteId", commuteId)).toList()

    override suspend fun findByAccommodationId(accommodationId: UUID): List<TravelOffer> =
        collection.find(Document("accommodationId", accommodationId)).toList()

    override suspend fun findByAttractionId(attractionId: UUID): List<TravelOffer> =
        collection.find(Document("attractionId", attractionId)).toList()
}
