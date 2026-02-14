package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.domain.event.AccommodationEvent
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.offermaker.LocalDateTimeRange
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import java.time.LocalDateTime

@Repository
class MongoAccommodationRepository(
    mongoConfiguration: MongoConfiguration
) : AccommodationRepository, AccommodationQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<Accommodation>("accommodations")

    override suspend fun findById(id: AccommodationId): Accommodation {
        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Accommodation not found: $id")
    }

    override suspend fun create(entity: Accommodation, event: AccommodationEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Accommodation, event: AccommodationEvent) {
        this.save(entity)
    }

    override suspend fun save(entity: Accommodation) {
        collection.replaceOne(
            Filters.eq("id", entity.id.value.toString()),
            entity,
            ReplaceOptions().upsert(true)
        )
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: AccommodationStatusEnum): List<Accommodation> {
        return collection.find(Filters.eq("status", status.name)).toList()
    }

    override suspend fun findByLocationAndDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Accommodation> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.toString()),
                dateRange.from?.let { Filters.gte("rent.from", it) },
                dateRange.till?.let { Filters.lte("rent.from", it) }
            )
        ).toList()
    }

    override suspend fun findByLocationAndRentContainsDate(
        location: LocationEnum,
        date: LocalDateTime
    ): List<Accommodation> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.toString()),
                Filters.gte("rent.from", date),
                Filters.lte("rent.till", date)
            )
        ).toList()
    }

    override suspend fun countTravelOfferByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum?
    ): Int {
        // TODO: Implement this method
        return 0
    }
}
