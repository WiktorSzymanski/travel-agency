package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import java.util.*

class TravelOfferQueryRepositoryImpl(
    database: MongoDatabase,
) : TravelOfferQueryRepository {
    private val collection: MongoCollection<TravelOffer> = database.getCollection("travelOffer")

    override suspend fun findTravelOfferDto(
        page: Int,
        size: Int,
        status: TravelOfferStatusEnum?,
        travelOfferId: UUID?,
        userId: UUID?,
    ): List<TravelOfferDto> {
        val entryFilters = listOfNotNull(
            travelOfferId?.let { Filters.eq("_id", it) },
            status?.let{ Filters.eq("status", it.name) },
            userId?.let { Filters.eq("booking.userId", it) }
        )

        val paginationSkip = Aggregates.skip((page - 1) * size)
        val paginationLimit = Aggregates.limit(size)

        val accommodationLookup = Aggregates.lookup(
            "accommodation",
            "accommodationId",
            "_id",
            "accommodation"
        )

        val attractionLookup = Aggregates.lookup(
            "attraction",
            "attractionId",
            "_id",
            "attraction"
        )

        val commuteLookup = Aggregates.lookup(
            "commute",
            "commuteId",
            "_id",
            "commute"
        )

        val projection = Aggregates.project(
            Projections.fields(
                Projections.include("_id", "name", "booking", "status"),
                Projections.computed("accommodation", Document("\$arrayElemAt", listOf("\$accommodation", 0))),
                Projections.computed("attraction", Document("\$arrayElemAt", listOf("\$attraction", 0))),
                Projections.computed("commute", Document("\$arrayElemAt", listOf("\$commute", 0)))
            )
        )

        val pipeline = listOfNotNull(
            entryFilters.takeIf { it.isNotEmpty() }?.let {
                Aggregates.match(Filters.and(it))
            },
            paginationSkip,
            paginationLimit,
            accommodationLookup,
            attractionLookup,
            commuteLookup,
            projection
        )

        return collection.aggregate<Document>(pipeline)
            .toList()
            .map{ it.toTravelOfferDto() }
    }
}
