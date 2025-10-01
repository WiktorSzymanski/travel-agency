package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.dto.ArrivalLocationDto
import pl.szymanski.wiktor.ta.dto.CommuteStatisticDto
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.queryRepository.CommuteCancelUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdate
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateRevision
import pl.szymanski.wiktor.ta.queryRepository.CommuteUpdateStatus
import pl.szymanski.wiktor.ta.withRetry
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class CommuteQueryRepositoryImpl() : CommuteQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

    private val container = runBlocking { CosmosClientProjectionProvider.getCommuteContainer() }

    override suspend fun save(entity: Commute): Commute? =
        try {
            container.createItem(entity).map { entity }.awaitSingleOrNull()
        } catch (ex: Exception) {
            log.error("Error inserting commute: $entity", ex)
            null
        }

    override suspend fun findById(commuteId: UUID): Commute {
        val query = "SELECT * FROM c WHERE c.id = @id"
        val params = listOf(SqlParameter("@id", commuteId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Commute::class.java)
            .awaitSingle()
            ?: throw NoSuchElementException("Commute with id $commuteId not found")
    }

    override suspend fun findAllByStatus(status: CommuteStatusEnum): List<Commute> {
        val query = "SELECT * FROM c WHERE c.status = @status"
        val params = listOf(SqlParameter("@status", status.toString()))
        val querySpec = SqlQuerySpec(query, params)
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Commute::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun update(entity: CommuteUpdate) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $current with update $entity")

            val bookings = current.bookings.toMutableMap()
            entity.bookingId?.let { bid -> bookings[bid.toString()] = entity.seat.toString() }
            val updated = current.copy(
                bookings = bookings,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.arrival.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: CommuteUpdateRevision) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $current with update $entity")

            val updated = current.copy(lastRevision = entity.revision)

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.arrival.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: CommuteCancelUpdate) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $current with update $entity")

            val bookings = current.bookings.toMutableMap()
            entity.bookingId?.let { bid -> bookings.remove(bid.toString()) }
            val updated = current.copy(
                bookings = bookings,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.arrival.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun update(entity: CommuteUpdateStatus) {
        withRetry {
            val current = findById(entity.id)
            if (current.lastRevision != entity.revision - 1) throw ConcurrentModificationException("Could not update $current with update $entity")

            val updated = current.copy(
                status = entity.status!!,
                lastRevision = entity.revision
            )

            runCatching {
                container.replaceItem(updated, entity.id.toString(), PartitionKey(current.arrival.location.toString())).awaitSingle()
            }.exceptionOrNull()?.let { throw Exception("Failed to update $entity revision", it) }
        }
    }

    override suspend fun findStatistics(
        page: Int,
        size: Int,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<CommuteStatisticDto> {
        // Fetch EXPIRED commutes; filter by date range in memory to avoid date serialization issues
        val query = "SELECT * FROM c WHERE c.status = @status"
        val params = listOf(SqlParameter("@status", CommuteStatusEnum.EXPIRED.toString()))
        val querySpec = SqlQuerySpec(query, params)
        val commutes = container.queryItems(querySpec, CosmosQueryRequestOptions(), Commute::class.java)
            .collectList()
            .awaitSingle()
            .filter { it.arrival.time in startDate..endDate }

        val grouped = commutes.groupBy {
            // Truncate to minute as in original Mongo pipeline
            it.arrival.time.withSecond(0).withNano(0)
        }

        val results = grouped.toSortedMap().map { (timeSlot, items) ->
            val byLocation = items.groupBy { it.arrival.location.toString() }
            val locations = byLocation.map { (loc, list) ->
                val bookingsCount = list.sumOf { it.bookings.size }
                ArrivalLocationDto(
                    location = loc,
                    commutesNumber = list.size,
                    passengersNumber = bookingsCount,
                )
            }
            CommuteStatisticDto(
                time = timeSlot.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                totalCommuteCount = items.size,
                totalBookingsCount = items.sumOf { it.bookings.size },
                arrivalLocations = locations,
            )
        }

        val fromIndex = ((page - 1) * size).coerceAtMost(results.size)
        val toIndex = (fromIndex + size).coerceAtMost(results.size)
        return results.subList(fromIndex, toIndex)
    }
}
