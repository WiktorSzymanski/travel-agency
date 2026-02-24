package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.EventEnvelope
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.BookingCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.repository.BookingRepository
import pl.szymanski.wiktor.ta.infrastructure.dto.BookingDto
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID

@Repository
class MongoBookingRepository(
    mongoConfiguration: MongoConfiguration
) : BookingRepository, BookingQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<BookingDto>("booking")

    override suspend fun findById(id: BookingId): Pair<Booking, Long> {
        val entity = collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Booking not found: $id")
        return entity.toDomain() to entity.version
    }

    override suspend fun create(entity: Booking, metadata: Metadata) {
        collection.insertOne(BookingDto.fromDomain(entity))
    }

    override suspend fun save(entity: Booking, metadata: Metadata) {
        val result = collection.replaceOne(
            Filters.and(
                Filters.eq("id", entity.id.value.toString()),
                Filters.eq("version", metadata.revision - 1)
            ),
            BookingDto.fromDomain(entity, metadata.revision),
            ReplaceOptions().upsert(false)
        )

        if (result.matchedCount == 0L) {
            throw IllegalStateException(
                "Optimistic locking failure: Booking ${entity.id} version mismatch. " +
                "Expected version ${metadata.revision - 1} but document was modified by another transaction."
            )
        }
    }

    override suspend fun save(entity: Booking) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }
}
