package pl.szymanski.wiktor.ta.infrastructure.repository.command

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProvider
import java.util.UUID

class BookingRepositoryImpl() : BookingRepository {
    private val container = runBlocking { CosmosClientProjectionProvider.getBookingContainer() }

    override suspend fun findById(bookingId: UUID): Booking {
        val query = "SELECT * FROM c WHERE c.id = @id"
        val params = listOf(SqlParameter("@id", bookingId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Booking::class.java)
            .awaitSingle()
            ?: throw NoSuchElementException("Accommodation with id $bookingId not found")
    }

    override suspend fun save(booking: Booking): Booking? =
        try {
            container.createItem(booking).map { booking }.awaitSingle()
        } catch (ex: Exception) {
            throw Exception("Error inserting booking: $booking", ex)
        }

    override suspend fun update(booking: Booking) {
        val current = findById(booking.id)

        if (current.version != booking.version) throw ConcurrentModificationException("Could not update $current with update $booking")

        runCatching {
            container.replaceItem(booking.copy(version = booking.version + 1), booking.id.toString(), PartitionKey(current.id.toString())).awaitSingle()
        }.exceptionOrNull()?.let { throw Exception("Failed to update $booking revision", it) }
    }

    override suspend fun findByUserId(userId: UUID): List<Booking> {
        val query = "SELECT * FROM c WHERE c.userId = @userId"
        val params = listOf(SqlParameter("@userId", userId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Booking::class.java)
            .collectList()
            .block()
            ?: throw NoSuchElementException("No Bookings for user with id $userId")
    }

    override suspend fun findByState(state: BookingState): List<Booking> {
        val query = "SELECT * FROM c WHERE c.status = @status"
        val params = listOf(SqlParameter("@status", state.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Booking::class.java)
            .collectList()
            .block()
            ?: throw NoSuchElementException("No Bookings in given status: $state")
    }
}