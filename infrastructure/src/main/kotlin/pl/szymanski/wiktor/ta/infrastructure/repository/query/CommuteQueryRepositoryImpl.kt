package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.queryRepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateStatus
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.dto.ArrivalLocationDto
import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import pl.szymanski.wiktor.ta.queryRepository.CommuteCancelUpdate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class CommuteQueryRepositoryImpl(
    database: MongoDatabase,
) : CommuteQueryRepository {
    private val collection: MongoCollection<Commute> = database.getCollection("commute")
    
    override suspend fun save(entity: Commute): Commute? = collection.insertOne(entity).insertedId?.let { entity }

    override suspend fun findById(commuteId: UUID): Commute = collection.find(Document("_id", commuteId)).toList().first()

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> = collection.find(Document("status", status.toString())).toList()

    override suspend fun update(entity: CommuteUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )

        val bookingIdStr = entity.bookingId.toString()
        val seatStr = entity.seat.toString()

        val update = Updates.set("bookings.$bookingIdStr", seatStr)

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun update(entity: CommuteCancelUpdate) {
        val filter = Filters.and(
            Filters.eq("_id", entity._id),
        )

        val bookingIdStr = entity.bookingId.toString()

        val update = Updates.unset("bookings.$bookingIdStr")

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Could not update ${entity._id}")
        }
    }

    override suspend fun update(entity: CommuteUpdateStatus) {
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

    override suspend fun findStatistics(
        page: Int,
        size: Int,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<CommuteStatisticDto> {
        val matchStage = Aggregates.match(Filters.eq("status", "EXPIRED"))
        val dateTruncExpr =
            Document(
                "\$dateTrunc",
                Document("date", "\$arrival.time")
                    .append("unit", "minute"),
            )

        val bookingsCountExpr =
            Document(
                "\$size",
                Document("\$objectToArray", "\$bookings"),
            )

        val projectStage =
            Aggregates.project(
                Projections.fields(
                    Projections.computed("commuteId", "\$_id"),
                    Projections.computed("arrivalLocation", "\$arrival.location"),
                    Projections.computed("arrivalTime", "\$arrival.time"),
                    Projections.computed("timeSlot", dateTruncExpr),
                    Projections.computed("bookingsCount", bookingsCountExpr),
                ),
            )

        val timeRangeMatch =
            Aggregates.match(
                Filters.and(
                    Filters.gte("arrivalTime", startDate),
                    Filters.lt("arrivalTime", endDate),
                ),
            )

        val firstGroupId =
            Document()
                .append("timeSlot", "\$timeSlot")
                .append("arrivalLocation", "\$arrivalLocation")

        val firstGroupStage =
            Aggregates.group(
                firstGroupId,
                Accumulators.sum("bookingsCount", "\$bookingsCount"),
                Accumulators.sum("commuteCount", 1),
            )

        val secondGroupStage =
            Aggregates.group(
                "\$_id.timeSlot",
                Accumulators.sum("allBookingsCount", "\$bookingsCount"),
                Accumulators.sum("allFlightsCount", "\$commuteCount"),
                Accumulators.push(
                    "locations",
                    Document()
                        .append("location", "\$_id.arrivalLocation")
                        .append("bookingsCount", "\$bookingsCount")
                        .append("flightsCount", "\$commuteCount"),
                ),
            )

        val paginationSkip = Aggregates.skip((page - 1) * size)
        val paginationLimit = Aggregates.limit(size)

        val sortStage = Aggregates.sort(Sorts.ascending("_id"))

        val pipeline =
            listOf(
                matchStage,
                projectStage,
                timeRangeMatch,
                firstGroupStage,
                secondGroupStage,
                paginationSkip,
                paginationLimit,
                sortStage,
            )

        return collection.aggregate<Document>(pipeline).toList().map { it.toCommuteStatisticDto() }
    }

    private fun Document.toCommuteStatisticDto(): CommuteStatisticDto {
        val timeSlotDate =
            this.get("_id") as? Date
                ?: throw IllegalArgumentException("Missing or invalid '_id' field")

        val time =
            Instant.ofEpochMilli(timeSlotDate.time)
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val locationsDocs = this.getList("locations", Document::class.java) ?: emptyList()

        val arrivalLocations =
            locationsDocs.map { locationDoc ->
                ArrivalLocationDto(
                    location = locationDoc.getString("location") ?: "Unknown",
                    commutesNumber = locationDoc.getInteger("flightsCount") ?: 0,
                    passengersNumber = locationDoc.getInteger("bookingsCount") ?: 0,
                )
            }

        return CommuteStatisticDto(
            time = time,
            totalCommuteCount = this.getInteger("allFlightsCount"),
            totalBookingsCount = this.getInteger("allBookingsCount"),
            arrivalLocations = arrivalLocations,
        )
    }
}
