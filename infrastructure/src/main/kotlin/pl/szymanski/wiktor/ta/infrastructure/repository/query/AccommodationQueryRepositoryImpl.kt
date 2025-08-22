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
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdate
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateStatus
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class AccommodationQueryRepositoryImpl(
    database: MongoDatabase,
) : AccommodationQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    val maxRetries = 10
    val initialDelayMs = 100L
    val maxDelayMs = 10000L
    val jitterFactor = 0.1

    private val collection: MongoCollection<Accommodation> = database.getCollection("accommodation")

    override suspend fun save(entity: Accommodation): Accommodation? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(accommodationId: UUID): Accommodation = collection.find(Document("_id", accommodationId)).firstOrNull() ?: throw NoSuchElementException()

    override suspend fun findAllByStatus(status: AccommodationStatusEnum): List<Accommodation> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: AccommodationUpdate) {
        val filter =
            Filters.and(
                Filters.eq("_id", entity._id),
                Filters.eq("lastRevision", entity.revision - 1),
            )
        val update =
            Updates.combine(
                Updates.set("bookingId", entity.bookingId),
                Updates.set("status", "${entity.status}"),
                Updates.set("lastRevision", entity.revision),
            )

        runCatching {
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
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }

    override suspend fun update(entity: AccommodationUpdateRevision) {
        val filter =
            Filters.and(
                Filters.eq("_id", entity._id),
                Filters.eq("lastRevision", entity.revision - 1),
            )

        val update =
            Updates.combine(
                Updates.set("lastRevision", entity.revision),
            )

        runCatching {
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
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }

    override suspend fun update(entity: AccommodationUpdateStatus) {
        val filter =
            Filters.and(
                Filters.eq("_id", entity._id),
                Filters.eq("lastRevision", entity.revision - 1),
            )
        val update =
            Updates.combine(
                Updates.set("status", "${entity.status}"),
                Updates.set("lastRevision", entity.revision),
            )

        runCatching {
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
        }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
    }

    override suspend fun findTravelOfferByLocation(
        page: Int,
        size: Int,
        location: LocationEnum,
        status: TravelOfferStatusEnum?,
    ): List<TravelOfferDto> {
        val accommodationStatus = status?.let { AccommodationStatusEnum.valueOf(status.toString()) }

        val entryFiltersMatch =
            Aggregates.match(
                Filters.and(
                    listOfNotNull(
                        Filters.eq("location", location),
                        accommodationStatus?.let { Filters.eq("status", accommodationStatus) },
                    ),
                ),
            )

        val paginationSkip = Aggregates.skip((page - 1) * size)
        val paginationLimit = Aggregates.limit(size)

        val travelOfferLookup =
            Aggregates.lookup(
                "travelOffer",
                "_id",
                "accommodationId",
                "travelOffer",
            )

        val travelOfferUnwind = Aggregates.unwind("\$travelOffer")

        val travelOfferStatusCheck =
            status?.let {
                Aggregates.match(
                    Filters.eq("travelOffer.status", status),
                )
            }

        val attractionLookup =
            Aggregates.lookup(
                "attraction",
                "travelOffer.attractionId",
                "_id",
                "attraction",
            )

        val commuteLookup =
            Aggregates.lookup(
                "commute",
                "travelOffer.commuteId",
                "_id",
                "commute",
            )

        val projection =
            Aggregates.project(
                Projections.fields(
                    Projections.computed("_id", "\$travelOffer._id"),
                    Projections.computed("name", "\$travelOffer.name"),
                    Projections.computed("status", "\$travelOffer.status"),
                    Projections.computed("booking", "\$travelOffer.booking"),
                    Projections
                        .computed(
                            "commute",
                            Document("\$arrayElemAt", listOf("\$commute", 0)),
                        ),
                    Projections.computed(
                        "accommodation",
                        Document().append("_id", "\$_id")
                            .append("name", "\$name")
                            .append("location", "\$location")
                            .append("rent", "\$rent")
                            .append("status", "\$status")
                            .append("booking", "\$booking"),
                    ),
                    Projections
                        .computed(
                            "attraction",
                            Document("\$arrayElemAt", listOf("\$attraction", 0)),
                        ),
                ),
            )

        val pipeline =
            listOfNotNull(
                entryFiltersMatch,
                paginationSkip,
                paginationLimit,
                travelOfferLookup,
                travelOfferUnwind,
                travelOfferStatusCheck.takeIf { it != null },
                attractionLookup,
                commuteLookup,
                projection,
            )

        return collection.aggregate<Document>(pipeline).toList().map { it.toTravelOfferDto() }
    }

    override suspend fun countTravelOfferByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum?,
    ): Int {
        val accommodationStatus = status?.let { AccommodationStatusEnum.valueOf(status.toString()) }

        val entryFiltersMatch =
            Aggregates.match(
                Filters.and(
                    listOfNotNull(
                        Filters.eq("location", location),
                        accommodationStatus?.let { Filters.eq("status", accommodationStatus) },
                    ),
                ),
            )

        val travelOfferLookup =
            Aggregates.lookup(
                "travelOffer",
                "_id",
                "accommodationId",
                "travelOffer",
            )

        val travelOfferUnwind = Aggregates.unwind("$" + "travelOffer")

        val travelOfferStatusCheck =
            status?.let {
                Aggregates.match(
                    Filters.eq("travelOffer.status", status),
                )
            }

        val countStage = Aggregates.count("count")

        val pipeline =
            listOfNotNull(
                entryFiltersMatch,
                travelOfferLookup,
                travelOfferUnwind,
                travelOfferStatusCheck.takeIf { it != null },
                countStage,
            )

        val result = collection.aggregate<Document>(pipeline).firstOrNull()
        return result?.getInteger("count") ?: 0
    }
}


