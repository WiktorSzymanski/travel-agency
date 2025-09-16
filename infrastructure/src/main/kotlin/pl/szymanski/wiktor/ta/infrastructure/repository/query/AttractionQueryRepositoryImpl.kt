package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.queryRepository.AttractionCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdate
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AttractionUpdateStatus
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class AttractionQueryRepositoryImpl() : AttractionQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

    private val container = runBlocking { CosmosClientProjectionProvider.getAttractionContainer() }

    override suspend fun save(entity: Attraction): Attraction? =
        try {
            container.createItem(entity).map { entity }.awaitSingleOrNull()
        } catch (ex: Exception) {
            log.error("Error inserting attraction: $entity", ex)
            null
        }

    override suspend fun findById(attractionId: UUID): Attraction {
        val query = "SELECT * FROM c WHERE c.id = @id"
        val params = listOf(SqlParameter("@id", attractionId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Attraction::class.java)
            .awaitSingle()
            ?: throw NoSuchElementException("Attraction with id $attractionId not found")
    }

    override suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction> {
        val query = "SELECT * FROM c WHERE c.status = @status"
        val params = listOf(SqlParameter("@status", status.toString()))
        val querySpec = SqlQuerySpec(query, params)
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Attraction::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun update(entity: AttractionUpdate) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $entity")

            val updatedBookings = current.bookings.toMutableList().apply {
                entity.bookingId?.let { if (!this.contains(it)) this.add(it) }
            }
            val updated = current.copy(
                bookings = updatedBookings,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: AttractionUpdateRevision) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $entity")

            val updatedBookings = current.bookings.toMutableList().apply {
                entity.bookingId?.let { if (!this.contains(it)) this.add(it) }
            }
            val updated = current.copy(
                bookings = updatedBookings,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: AttractionCancelUpdate) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $entity")

            val updatedBookings = current.bookings.toMutableList().apply {
                entity.bookingId?.let { this.remove(it) }
            }
            val updated = current.copy(
                bookings = updatedBookings,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: AttractionUpdateStatus) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $entity")

            val updated = current.copy(
                status = entity.status!!,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }
}