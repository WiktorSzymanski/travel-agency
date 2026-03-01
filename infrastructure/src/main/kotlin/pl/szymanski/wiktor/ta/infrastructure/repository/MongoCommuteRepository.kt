package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.infrastructure.document.CommuteDocument
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate

@Repository
class MongoCommuteRepository(
    mongoConfiguration: MongoConfiguration
) : CommandRepository<Commute, CommuteId>, CommuteQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<CommuteDocument>("commutes")

    override suspend fun findById(id: CommuteId): Pair<Commute, Long> {
        val entity = collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Commute not found: $id")
        return entity.toDomain() to entity.version
    }

    override suspend fun create(entity: Commute, metadata: Metadata) {
        collection.insertOne(CommuteDocument.fromDomain(entity))
    }

    override suspend fun save(entity: Commute, metadata: Metadata) {
        val result = collection.replaceOne(
            Filters.and(
                Filters.eq("id", entity.id.value.toString()),
                Filters.eq("version", metadata.revision - 1)
            ),
            CommuteDocument.fromDomain(entity, metadata.revision),
            ReplaceOptions().upsert(false)
        )

        if (result.matchedCount == 0L) {
            throw IllegalStateException(
                "Optimistic locking failure: Commute ${entity.id} version mismatch. " +
                "Expected version ${metadata.revision - 1} but document was modified by another transaction."
            )
        }
    }

    override suspend fun save(entity: Commute) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> {
        return collection.find(Filters.eq("status", status.name)).toList().map { it.toDomain() }
    }

    override suspend fun findByLocationAndArrivalDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Commute> {
        return collection.find(
            Filters.and(
                Filters.eq("arrival.location", location.name),
                dateRange.from?.let { Filters.gte("arrival.time", it.toString()) },
                dateRange.till?.let { Filters.lte("arrival.time", it.toString()) }
            )
        ).toList().map { it.toDomain() }
    }
}
