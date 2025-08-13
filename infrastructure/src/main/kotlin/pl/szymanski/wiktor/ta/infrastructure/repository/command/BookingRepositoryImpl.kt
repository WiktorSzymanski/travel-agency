package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import java.util.UUID

class BookingRepositoryImpl(
    database: MongoDatabase,
) : BookingRepository {
    private val collection: MongoCollection<Booking> = database.getCollection("booking")

    override suspend fun findById(bookingId: UUID): Booking = collection.find(Document("_id", bookingId)).toList().first()

    override suspend fun save(booking: Booking): Booking {
        val insertId = collection.insertOne(booking)
        return insertId.let{ booking }
    }

    override suspend fun update(booking: Booking) {
        val filter =
            Filters.and(
                Filters.eq("_id", booking._id),
                Filters.eq("version", booking.version),
            )
        val update =
            Updates.combine(
                Updates.set("status", "${booking.status}"),
                Updates.set("message", booking.message),
                Updates.set("version", booking.version + 1),
            )

        if (collection.updateOne(filter, update).matchedCount == 0L) {
            throw ConcurrentModificationException("Concurrent modification detected for ${booking._id}")
        }
    }

    override suspend fun findByUserId(userId: UUID): List<Booking> =
        collection.find(Document("userId", userId)).toList()

    override suspend fun findByState(state: BookingState): List<Booking> =
        collection.find(Document("state", state.toString())).toList()
}