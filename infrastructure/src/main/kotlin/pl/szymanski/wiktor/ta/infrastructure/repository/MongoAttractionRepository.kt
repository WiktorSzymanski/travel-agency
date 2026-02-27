package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.infrastructure.dto.AttractionDto
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate

@Repository
class MongoAttractionRepository(
    mongoConfiguration: MongoConfiguration
) : CommandRepository<Attraction, AttractionId>, AttractionQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<AttractionDto.Present>("attractions")

    override suspend fun findById(id: AttractionId): Pair<Attraction, Long> {
        if (id !is AttractionId.Present) throw NoSuchElementException("Attraction not found: $id")

        val entity = collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Attraction not found: $id")
        return entity.toDomain() to entity.version
    }

    override suspend fun create(entity: Attraction, metadata: Metadata) {
        collection.insertOne(AttractionDto.fromDomain(entity) as AttractionDto.Present)
    }

    override suspend fun save(entity: Attraction, metadata: Metadata) {
        val result = collection.replaceOne(
            Filters.and(
                Filters.eq("id", entity.id.value.toString()),
                Filters.eq("version", metadata.revision - 1)
            ),
            AttractionDto.fromDomain(entity, metadata.revision) as AttractionDto.Present,
            ReplaceOptions().upsert(false)
        )

        if (result.matchedCount == 0L) {
            throw IllegalStateException(
                "Optimistic locking failure: Attraction ${entity.id} version mismatch. " +
                "Expected version ${metadata.revision - 1} but document was modified by another transaction."
            )
        }
    }

    override suspend fun save(entity: Attraction) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> {
        return collection
            .find(Filters.eq("status", status.toString()))
            .toList()
            .map { it.toDomain() }
    }

    override suspend fun findByLocationAndDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Attraction> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.name),
                dateRange.from?.let { Filters.gte("date", it.toString()) },
                dateRange.till?.let { Filters.lte("date", it.toString()) }
            )
        ).toList().map { it.toDomain() }
    }
}
