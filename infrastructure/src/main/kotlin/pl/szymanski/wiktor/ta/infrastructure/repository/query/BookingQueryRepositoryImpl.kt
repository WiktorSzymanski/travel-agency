package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.queryRepository.BookingQueryRepository
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import java.util.UUID

class BookingQueryRepositoryImpl(
    database: MongoDatabase,
) : BookingQueryRepository {
    private val collection: MongoCollection<Booking> = database.getCollection("booking")

    override suspend fun findTravelOfferDtoByUserId(
        page: Int,
        size: Int,
        userId: UUID
    ): List<TravelOfferDto> {
//        val matchStage = Aggregates.match(
//            Filters.and(
//                Filters.eq("status", "SUCCEEDED"),
//                Filters.eq("userId", userId),
//            )
//        )
//
//        val paginationSkip = Aggregates.skip((page - 1) * size)
//        val paginationLimit = Aggregates.limit(size)
//
//        val travelOfferLookup = Aggregates.lookup(
//            "travelOffer",
//            "travelOfferId",
//            "id",
//            "travelOffer"
//        )
//
//        val replaceRootStage = Aggregates.replaceRoot(
//            Document("\$arrayElemAt", listOf("\$travelOffer", 0))
//        )
//
//        val accommodationLookup =
//            Aggregates.lookup(
//                "accommodation",
//                "accommodationId",
//                "id",
//                "accommodation",
//            )
//
//        val attractionLookup =
//            Aggregates.lookup(
//                "attraction",
//                "attractionId",
//                "id",
//                "attraction",
//            )
//
//        val commuteLookup =
//            Aggregates.lookup(
//                "commute",
//                "commuteId",
//                "id",
//                "commute",
//            )
//
//        val projection =
//            Aggregates.project(
//                Projections.fields(
//                    Projections.include("id", "name", "booking", "status"),
//                    Projections.computed("accommodation", Document("\$arrayElemAt", listOf("\$accommodation", 0))),
//                    Projections.computed("attraction", Document("\$arrayElemAt", listOf("\$attraction", 0))),
//                    Projections.computed("commute", Document("\$arrayElemAt", listOf("\$commute", 0))),
//                ),
//            )
//
//
//        val pipeline = listOf(
//            matchStage,
//            paginationSkip,
//            paginationLimit,
//            travelOfferLookup,
//            replaceRootStage,
//            accommodationLookup,
//            attractionLookup,
//            commuteLookup,
//            projection,
//        )
//
//        return collection.aggregate<Document>(pipeline)
//            .toList()
//            .map { it.toTravelOfferDto() }
        throw Exception("Not yet implemented")
    }

    override suspend fun findById(bookingId: UUID): Booking = collection.find(Document("id", bookingId)).firstOrNull() ?: throw NoSuchElementException()

    override suspend fun findByUserId(
        page: Int,
        size: Int,
        userId: UUID): List<Booking> =
        collection
            .find(Document("userId", userId))
            .skip((page - 1) * size)
            .limit(size)
            .toList()
}