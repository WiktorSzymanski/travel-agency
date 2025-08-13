package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdate
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateStatus
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import java.util.UUID

class TravelOfferQueryRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferQueryRepository {
    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")
    
    override suspend fun save(entity: TravelOffer): TravelOffer? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(travelOfferId: UUID): TravelOffer = collection.find(Document("_id", travelOfferId)).toList().first()

    override suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: TravelOfferUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )
        
        val updatesList = mutableListOf<Bson>()
        
        if (entity.status != null) {
            updatesList.add(Updates.set("status", "${entity.status}"))
        }
        
        // Handle bookingId for TravelOffer
        if (entity.bookingId != null) {
            // For TravelOffer, we store a single bookingId
            updatesList.add(Updates.set("bookingId", entity.bookingId))
        } else {
            // If bookingId is null, we're removing the booking
            updatesList.add(Updates.set("bookingId", null))
        }
        
        val update = Updates.combine(updatesList)

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun update(entity: TravelOfferUpdateStatus) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )
        val update = Updates.combine(
            Updates.set("status", "${entity.status}"),
        )

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun findTravelOfferDto(
        page: Int,
        size: Int,
        status: TravelOfferStatusEnum?,
        travelOfferId: UUID?,
    ): List<TravelOfferDto> {
        val entryFilters =
            listOfNotNull(
                travelOfferId?.let { Filters.eq("_id", it) },
                status?.let { Filters.eq("status", it.name) },
            )

        val paginationSkip = Aggregates.skip((page - 1) * size)
        val paginationLimit = Aggregates.limit(size)

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
                    Projections.include("_id", "name", "booking", "status"),
                    Projections.computed("accommodation", Document("\$arrayElemAt", listOf("\$accommodation", 0))),
                    Projections.computed("attraction", Document("\$arrayElemAt", listOf("\$attraction", 0))),
                    Projections.computed("commute", Document("\$arrayElemAt", listOf("\$commute", 0))),
                ),
            )

        val pipeline =
            listOfNotNull(
                entryFilters.takeIf { it.isNotEmpty() }?.let {
                    Aggregates.match(Filters.and(it))
                },
                paginationSkip,
                paginationLimit,
                accommodationLookup,
                attractionLookup,
                commuteLookup,
                projection,
            )

        return collection.aggregate<Document>(pipeline)
            .toList()
            .map { it.toTravelOfferDto() }
    }
    
    override suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int {
        val filter = Filters.eq("status", status.name)
        return collection.countDocuments(filter).toInt()
    }

    override suspend fun findByCommuteId(commuteId: UUID): List<UUID> =
        collection.find(Document("commuteId", commuteId))
            .toList()
            .map { it._id }

    override suspend fun findByAccommodationId(accommodationId: UUID): List<UUID> =
        collection.find(Document("accommodationId", accommodationId))
            .toList()
            .map { it._id }

    override suspend fun findByAttractionId(attractionId: UUID): List<UUID> =
        collection.find(Document("attractionId", attractionId))
            .toList()
            .map { it._id }

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
