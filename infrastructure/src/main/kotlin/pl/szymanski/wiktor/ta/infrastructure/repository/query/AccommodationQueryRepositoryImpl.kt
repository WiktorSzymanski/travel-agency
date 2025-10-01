package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.queryRepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdate
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.AccommodationUpdateStatus
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class AccommodationQueryRepositoryImpl() : AccommodationQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }
    private val container = runBlocking { CosmosClientProjectionProvider.getAccommodationContainer() }

    override suspend fun save(entity: Accommodation): Accommodation? =
        try {
            container.createItem(entity).map { entity }.awaitSingleOrNull()
        } catch (ex: Exception) {
            log.error("Error inserting accommodation: $entity", ex)
            null
        }

    override suspend fun findById(accommodationId: UUID): Accommodation {
        val query = "SELECT * FROM c WHERE c.id = @id"
        val params = listOf(SqlParameter("@id", accommodationId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Accommodation::class.java)
            .awaitSingle()
            ?: throw NoSuchElementException("Accommodation with id $accommodationId not found")
    }

    override suspend fun findAllByStatus(status: AccommodationStatusEnum): List<Accommodation> {
        val query = "SELECT * FROM c WHERE c.status = @status"
        val options = CosmosQueryRequestOptions()
        val params = listOf(
            SqlParameter("@status", status.toString())
        )

        val querySpec = SqlQuerySpec(query, params)
        return container.queryItems(querySpec, options, Accommodation::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun update(entity: AccommodationUpdate) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) ConcurrentModificationException("Could not update $current with update $entity")

            val updated = current.copy(
                bookingId = entity.bookingId,
                status = entity.status!!,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: AccommodationUpdateRevision) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) ConcurrentModificationException("Could not update $current with update $entity")

            val updated = current.copy(lastRevision = entity.revision)

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: AccommodationUpdateStatus) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) ConcurrentModificationException("Could not update $current with update $entity")

            val updated = current.copy(
                status = entity.status!!,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { log.error("Failed to update $entity revision", it) }
        }
    }

    override suspend fun findTravelOfferByLocation(
        page: Int,
        size: Int,
        location: LocationEnum,
        status: TravelOfferStatusEnum?,
    ): List<TravelOfferDto> {
//        val baseQuery = buildString {
//            append("SELECT * FROM c WHERE c.location = @location")
//            status?.let { append(" AND c.status = @status") }
//            append(" OFFSET @skip LIMIT @limit")
//        }
//
//        val params = listOf(
//            SqlParameter("@location", location.toString()),
//            SqlParameter("@skip", ((page - 1) * size)),
//            SqlParameter("@limit", size),
//            takeIf { status != null }?.let { SqlParameter("@status", status) }
//        )
//
//        val querySpec = SqlQuerySpec(baseQuery, params)
//
//        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Document::class.java)
//            .collectList()
//            .awaitSingle()
//            .map { it.toTravelOfferDto() }
        throw Exception("Not yet implemented")
    }

    override suspend fun countTravelOfferByLocation(location: LocationEnum, status: TravelOfferStatusEnum?): Int {
        val baseQuery = buildString {
            append("SELECT VALUE COUNT(1) FROM c WHERE c.location = @location")
            status?.let { append(" AND c.status = @status") }
        }
        val params = listOf(
            SqlParameter("@location", location.toString()),
            takeIf { status != null }?.let { SqlParameter("@status", status) }
        )

        val querySpec = SqlQuerySpec(baseQuery, params)
        val result = container.queryItems(querySpec, CosmosQueryRequestOptions(), Int::class.javaObjectType)
            .collectList()
            .awaitSingle()

        return result.firstOrNull() ?: 0
    }
}
