package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.repository.TravelOfferRepository
import java.util.UUID
import kotlin.ConcurrentModificationException

class TravelOfferRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferRepository {
    // db.travelOffer.createIndex( {commuteId: 1, attractionId: 1, accommodationId: 1 }, {unique: true} )
    companion object {
        const val DUPLICATE_ERROR_CODE = 11000
    }

    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findById(travelOfferId: UUID): TravelOffer = collection.find(Document("_id", travelOfferId)).toList().first()

    override suspend fun save(travelOffer: TravelOffer): TravelOffer? = collection.insertOne(travelOffer).insertedId?.let { travelOffer }

    override suspend fun update(travelOffer: TravelOffer) {
        val filter =
            Filters.and(
                Filters.eq("_id", travelOffer._id),
                Filters.eq("version", travelOffer.version),
            )
        val update =
            Updates.combine(
                Updates.set("bookingId", travelOffer.bookingId),
                Updates.set("status", "${travelOffer.status}"),
                Updates.set("version", travelOffer.version + 1),
            )
        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Concurrent modification detected for ${travelOffer._id}")
        }
    }

    override suspend fun findByCommuteId(commuteId: UUID): List<TravelOffer> = collection.find(Document("commuteId", commuteId)).toList()

    override suspend fun findByAccommodationId(accommodationId: UUID): List<TravelOffer> =
        collection.find(Document("accommodationId", accommodationId)).toList()

    override suspend fun findByAttractionId(attractionId: UUID): List<TravelOffer> =
        collection.find(Document("attractionId", attractionId)).toList()

    override suspend fun findStatusesOfComponents(
        travelOfferId: UUID,
    ): Triple<CommuteStatusEnum, AccommodationStatusEnum, AttractionStatusEnum?>? {
        val idMatcher = Aggregates.match(Filters.eq("_id", travelOfferId))

        val accommodationLookup =
            Aggregates.lookup(
                "accommodation",
                "accommodationId",
                "_id",
                "accommodation",
            )

        val attractionLookup =
            Aggregates.lookup(
                "attraction",
                "attractionId",
                "_id",
                "attraction",
            )

        val commuteLookup =
            Aggregates.lookup(
                "commute",
                "commuteId",
                "_id",
                "commute",
            )

        val projection =
            Aggregates.project(
                Projections.fields(
                    Projections.computed("accommodationStatus", Document("\$arrayElemAt", listOf("\$accommodation.status", 0))),
                    Projections.computed("attractionStatus", Document("\$arrayElemAt", listOf("\$attraction.status", 0))),
                    Projections.computed("commuteStatus", Document("\$arrayElemAt", listOf("\$commute.status", 0))),
                ),
            )

        val pipeline =
            listOfNotNull(
                idMatcher,
                accommodationLookup,
                attractionLookup,
                commuteLookup,
                projection,
            )

        return collection.aggregate<Document>(pipeline).toList().firstOrNull()?.let { doc ->
            Triple(
                CommuteStatusEnum.valueOf(doc.getString("commuteStatus")),
                AccommodationStatusEnum.valueOf(doc.getString("accommodationStatus")),
                doc.getString("attractionStatus")?.let { AttractionStatusEnum.valueOf(it) },
            )
        }
    }
}
