package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.domain.event.AttractionEvent
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.offermaker.LocalDateTimeRange
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate

@Repository
class MongoAttractionRepository(
    mongoConfiguration: MongoConfiguration
) : AttractionRepository, AttractionQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<Attraction>("attractions")

    override suspend fun findById(id: AttractionId): Attraction {
        if (id !is AttractionId.Present) throw NoSuchElementException("Attraction not found: $id")

        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Attraction not found: $id")
    }

    override suspend fun create(entity: Attraction, event: AttractionEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Attraction, event: AttractionEvent) {
        this.save(entity)
    }

    override suspend fun save(entity: Attraction) {
        val id = (entity.id as AttractionId.Present).value.toString()
        collection.replaceOne(
            Filters.eq("id", id),
            entity,
            ReplaceOptions().upsert(true)
        )
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> {
        return collection
            .find(Filters.eq("status", status.toString()))
            .toList()
    }

    override suspend fun findByLocationAndDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Attraction> {
        return collection.find(
            Filters.and(
                Filters.eq("location", location.toString()),
                dateRange.from?.let { Filters.gte("date", it) },
                dateRange.till?.let { Filters.lte("date", it) }
            )
        ).toList()
    }
}
