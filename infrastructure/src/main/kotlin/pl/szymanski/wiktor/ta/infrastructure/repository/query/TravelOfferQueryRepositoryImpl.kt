package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
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
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateRevision
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class TravelOfferQueryRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    val maxRetries = 10
    val initialDelayMs = 100L
    val maxDelayMs = 10000L
    val jitterFactor = 0.1

    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")
    
    override suspend fun save(entity: TravelOffer): TravelOffer? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(travelOfferId: UUID): TravelOffer = collection.find(Document("_id", travelOfferId)).firstOrNull() ?: throw NoSuchElementException()

    override suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: TravelOfferUpdateRevision, event: Event) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.lastRevision - 1),
        )
        val update = Updates.combine(
            Updates.addToSet("events", event.toString()),
            Updates.set("lastRevision", entity.lastRevision )
        )

        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
        }
    }

    override suspend fun update(entity: TravelOfferUpdate, event: Event) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.lastRevision - 1),
        )
        val update = Updates.combine(
            Updates.set("status", "${entity.status}"),
            Updates.addToSet("events", event.toString()),
            Updates.set("lastRevision", entity.lastRevision )
        )

        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
        }
    }

    override suspend fun update(entity: TravelOfferUpdateStatus, event: Event) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
            Filters.eq("lastRevision", entity.lastRevision - 1),
        )
        val update = Updates.combine(
            Updates.set("status", "${entity.status}"),
            Updates.addToSet("events", event.toString()),
            Updates.set("lastRevision", entity.lastRevision )
        )

        withRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor
        ) {
            if (collection.updateOne(filter, update).matchedCount == 0L) {
                throw ConcurrentModificationException("Could not update ${entity}")
            }
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
