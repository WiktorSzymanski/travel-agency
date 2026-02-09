package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.domain.event.BookingEvent
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseProvider

class MongoBookingRepository(
    private val databaseProvider: DatabaseProvider,
) : BookingRepository {
    @Serializable
    private data class BookingEventDocument(
        val aggregateId: String,
        val event: BookingEvent,
    )

    private val collection = databaseProvider.mongoClient
        .getDatabase(databaseProvider.mongoConfig.dbName)
        .getCollection<BookingEventDocument>("booking_events")

    override suspend fun findById(id: BookingId): Booking {
        val aggregateId = when (id) {
            is pl.szymanski.wiktor.ta.domain.aggregate.BookingId.Present -> id.value.toString()
            pl.szymanski.wiktor.ta.domain.aggregate.BookingId.Empty -> throw NoSuchElementException("Booking not found: $id")
        }
        val docs = collection
            .find(Filters.eq("aggregateId", aggregateId))
            .sort(Sorts.ascending("_id"))
            .toList()

        val events = docs.map { it.event }
        if (events.isEmpty()) throw NoSuchElementException("Booking not found: $id")
        return Booking.fromEvents(events)
    }

    override suspend fun create(entity: Booking, event: BookingEvent) {
        val aggregateId = (entity.id as pl.szymanski.wiktor.ta.domain.aggregate.BookingId.Present).value.toString()
        collection.insertOne(
            BookingEventDocument(
                aggregateId = aggregateId,
                event = event,
            )
        )
    }

    override suspend fun save(entity: Booking, event: BookingEvent) {
        val aggregateId = (entity.id as pl.szymanski.wiktor.ta.domain.aggregate.BookingId.Present).value.toString()
        collection.insertOne(
            BookingEventDocument(
                aggregateId = aggregateId,
                event = event,
            )
        )
    }
}
