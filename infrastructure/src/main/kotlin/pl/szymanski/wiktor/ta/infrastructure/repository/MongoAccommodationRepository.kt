package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.infrastructure.dto.AccommodationDto
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.AccommodationDtoMongoRepository
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import pl.szymanski.wiktor.ta.repository.CommandRepository
import java.time.LocalDateTime

@Component
class MongoAccommodationRepository(
    mongoConfiguration: MongoConfiguration,
    private val accommodationDtoMongoRepository: AccommodationDtoMongoRepository
) : CommandRepository<Accommodation, AccommodationId>, AccommodationQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<AccommodationDto>("accommodations")

    override suspend fun findById(id: AccommodationId): Pair<Accommodation, Long> {
        val entity = collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Accommodation not found: $id")
        return entity.toDomain() to entity.version
    }

    override suspend fun create(entity: Accommodation, metadata: Metadata) {
        collection.insertOne(AccommodationDto.fromDomain(entity))
    }

    override suspend fun save(entity: Accommodation, metadata: Metadata) {
        val result = collection.replaceOne(
            Filters.and(
                Filters.eq("id", entity.id.value.toString()),
                Filters.eq("version", metadata.revision - 1)
            ),
            AccommodationDto.fromDomain(entity, metadata.revision),
            ReplaceOptions().upsert(false)
        )

        if (result.matchedCount == 0L) {
            throw IllegalStateException(
                "Optimistic locking failure: Accommodation ${entity.id} version mismatch. " +
                "Expected version ${metadata.revision - 1} but document was modified by another transaction."
            )
        }
    }

    override suspend fun save(entity: Accommodation) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: AccommodationStatusEnum, pageable: Pageable): Page<Accommodation> =
        withContext(Dispatchers.IO) {
            accommodationDtoMongoRepository
                .findAccommodationDtosByStatus(status.toString(), pageable.toSpring())
                .toApplication { it.toDomain() }
        }

    override suspend fun findByLocationAndDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Accommodation> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.name),
                dateRange.from?.let { Filters.gte("rent.from", it.toString()) },
                dateRange.till?.let { Filters.lte("rent.from", it.toString()) }
            )
        ).toList().map { it.toDomain() }
    }

    override suspend fun findByLocationAndRentContainsDate(
        location: LocationEnum,
        date: LocalDateTime
    ): List<Accommodation> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.name),
                Filters.lte("rent.from", date.toString()),
                Filters.gte("rent.till", date.toString())
            )
        ).toList().map { it.toDomain() }
    }

    override suspend fun countTravelOfferByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum?
    ): Int {
        // TODO: Implement this method
        return 0
    }
}
