package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.domain.event.CommuteEvent
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.infrastructure.dto.CommuteStatisticDto
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.offermaker.LocalDateTimeRange
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import java.time.LocalDateTime

@Repository
class MongoCommuteRepository(
    mongoConfiguration: MongoConfiguration
) : CommuteRepository, CommuteQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<Commute>("commutes")

    override suspend fun findById(id: CommuteId): Commute {
        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Commute not found: $id")
    }

    override suspend fun create(entity: Commute, event: CommuteEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Commute, event: CommuteEvent) {
        this.save(entity)
    }

    override suspend fun save(entity: Commute) {
        collection.replaceOne(
            Filters.eq("id", entity.id.value.toString()),
            entity,
            ReplaceOptions().upsert(true)
        )
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> {
        return collection.find(Filters.eq("status", status.name)).toList()
    }

    override suspend fun findByLocationAndArrivalDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Commute> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.toString()),
                dateRange.from?.let { Filters.gte("departure.time", it) },
                dateRange.till?.let { Filters.lte("departure.time", it) }
            )
        ).toList()
    }
}
