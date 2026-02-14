package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.infrastructure.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID

@Repository
class MongoBookingRepository(
    mongoConfiguration: MongoConfiguration
) : BookingRepository, BookingQueryRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<Booking>("booking_events")

    override suspend fun findById(id: BookingId): Booking {
        return collection
            .find(Filters.eq("id", id.value.toString()))
            .firstOrNull()
            ?: throw NoSuchElementException("Booking not found: $id")
    }

    override suspend fun create(entity: Booking, event: BookingEvent) {
        collection.insertOne(entity)
    }

    override suspend fun save(entity: Booking, event: BookingEvent) {
        this.save(entity)
    }

    override suspend fun save(entity: Booking) {
        collection.replaceOne(
            Filters.eq("id", entity.id.value.toString()),
            entity,
            ReplaceOptions().upsert(true)
        )
    }
}
