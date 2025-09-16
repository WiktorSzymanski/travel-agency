package pl.szymanski.wiktor.ta.infrastructure.repository.query

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdate
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateStatus
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.Event
import pl.szymanski.wiktor.ta.dto.TravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toDto
import pl.szymanski.wiktor.ta.infrastructure.repository.toTravelOfferDto
import pl.szymanski.wiktor.ta.infrastructure.scheduler.CosmosClientProjectionProvider
import pl.szymanski.wiktor.ta.queryRepository.TravelOfferUpdateRevision
import pl.szymanski.wiktor.ta.withRetry
import java.util.UUID

class TravelOfferQueryRepositoryImpl() : TravelOfferQueryRepository {
    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    }

    private val container = runBlocking { CosmosClientProjectionProvider.getTravelOfferContainer() }
    private val accommodationContainer = runBlocking { CosmosClientProjectionProvider.getAccommodationContainer() }
    private val attractionContainer = runBlocking { CosmosClientProjectionProvider.getAttractionContainer() }
    private val commuteContainer = runBlocking { CosmosClientProjectionProvider.getCommuteContainer() }


    override suspend fun save(entity: TravelOffer): TravelOffer? =
        try {
            container.createItem(entity).map { entity }.awaitSingleOrNull()
        } catch (ex: Exception) {
            log.error("Error inserting travel offer: $entity", ex)
            null
        }

    override suspend fun findById(travelOfferId: UUID): TravelOffer {
        val query = "SELECT * FROM c WHERE c.id = @id"
        val params = listOf(SqlParameter("@id", travelOfferId.toString()))
        val querySpec = SqlQuerySpec(query, params)

        return container.queryItems(querySpec, CosmosQueryRequestOptions(), TravelOffer::class.java)
            .awaitSingle()
            ?: throw NoSuchElementException("Travel offer with id $travelOfferId not found")
    }

    override suspend fun findAllByStatus(status: TravelOfferStatusEnum): List<TravelOffer> {
        val query = "SELECT * FROM c WHERE c.status = @status"
        val params = listOf(SqlParameter("@status", status.toString()))
        val querySpec = SqlQuerySpec(query, params)
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), TravelOffer::class.java)
            .collectList()
            .awaitSingle()
            .toList()
    }

    override suspend fun update(entity: TravelOfferUpdateRevision, event: Event) {
        val current = findById(entity.id)
        if (current.lastRevision != entity.lastRevision - 1) throw ConcurrentModificationException("Could not update $entity")

        val updated = current.copy(
            lastRevision = entity.lastRevision
        )

        runCatching {
            container.replaceItem(updated, entity.id.toString(), PartitionKey(entity.id.toString())).awaitSingle()
        }.exceptionOrNull()?.let {throw Exception("Failed to update $entity revision", it)}
    }

    override suspend fun update(entity: TravelOfferUpdate, event: Event) {
        val current = findById(entity.id)
        if (current.lastRevision != entity.lastRevision - 1) throw ConcurrentModificationException("Could not update $entity")

        val updated = current.copy(
            status = entity.status!!,
            lastRevision = entity.lastRevision,
            bookingId = entity.bookingId,
        )

        runCatching {
            container.replaceItem(updated, entity.id.toString(), PartitionKey(entity.id.toString())).awaitSingle()
        }.exceptionOrNull()?.let {throw Exception("Failed to update $entity revision", it)}
    }

    override suspend fun update(entity: TravelOfferUpdateStatus, event: Event) {
        val current = findById(entity.id)
        if (current.lastRevision != entity.lastRevision - 1) throw ConcurrentModificationException("Could not update $entity")

        val updated = current.copy(
            status = entity.status!!,
            lastRevision = entity.lastRevision
        )

        runCatching {
            container.replaceItem(updated, entity.id.toString(), PartitionKey(entity.id.toString())).awaitSingle()
        }.exceptionOrNull()?.let {throw Exception("Failed to update $entity revision", it)}
    }

    suspend fun findAccommodationById(container: CosmosAsyncContainer, id: UUID): Accommodation? {
        val querySpec = SqlQuerySpec("SELECT * FROM c WHERE c.id = @id", listOf(SqlParameter("@id", id.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Accommodation::class.java).singleOrEmpty().awaitSingleOrNull()
    }

    suspend fun findAttractionById(container: CosmosAsyncContainer, id: UUID): Attraction? {
        val querySpec = SqlQuerySpec("SELECT * FROM c WHERE c.id = @id", listOf(SqlParameter("@id", id.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Attraction::class.java).singleOrEmpty().awaitSingleOrNull()
    }

    suspend fun findCommuteById(container: CosmosAsyncContainer, id: UUID): Commute? {
        val querySpec = SqlQuerySpec("SELECT * FROM c WHERE c.id = @id", listOf(SqlParameter("@id", id.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), Commute::class.java).singleOrEmpty().awaitSingleOrNull()
    }

    override suspend fun findTravelOfferDto(
        page: Int,
        size: Int,
        status: TravelOfferStatusEnum?,
        travelOfferId: UUID?,
    ): List<TravelOfferDto> {
        val querySb = StringBuilder("SELECT * FROM c WHERE 1=1")
        val params = mutableListOf<SqlParameter>()
        travelOfferId?.let {
            querySb.append(" AND c.id = @id")
            params.add(SqlParameter("@id", it.toString()))
        }
        status?.let {
            querySb.append(" AND c.status = @status")
            params.add(SqlParameter("@status", it.name))
        }
        querySb.append(" OFFSET @skip LIMIT @limit")
        params.add(SqlParameter("@skip", ((page - 1) * size)))
        params.add(SqlParameter("@limit", size))

        val querySpec = SqlQuerySpec(querySb.toString(), params)
        val offers = container.queryItems(querySpec, CosmosQueryRequestOptions(), TravelOffer::class.java)
            .collectList()
            .awaitSingle()

        // Manually fetch related data using container references
        return offers.map { offer ->
            val accommodation = findAccommodationById(accommodationContainer, offer.accommodationId)
            val attraction    = offer.attractionId?.let { findAttractionById(attractionContainer, it) }
            val commute      = findCommuteById(commuteContainer, offer.commuteId)
            offer.toTravelOfferDto(accommodation?.toDto(), attraction?.toDto(), commute?.toDto())
        }
    }


    override suspend fun countTravelOffersByStatus(status: TravelOfferStatusEnum): Int {
        val querySpec = SqlQuerySpec("SELECT VALUE COUNT(1) FROM c WHERE c.status = @status", listOf(SqlParameter("@status", status.name)))
        val result = container.queryItems(querySpec, CosmosQueryRequestOptions(), Int::class.javaObjectType)
            .collectList()
            .awaitSingle()
        return result.firstOrNull() ?: 0
    }


    override suspend fun findByCommuteId(commuteId: UUID): List<UUID> {
        val querySpec = SqlQuerySpec("SELECT c.id FROM c WHERE c.commuteId = @commuteId", listOf(SqlParameter("@commuteId", commuteId.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), UUID::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun findByAccommodationId (accommodationId: UUID): List<UUID> {
        val querySpec = SqlQuerySpec("SELECT c.id FROM c WHERE c.accommodationId = @accommodationId", listOf(SqlParameter("@accommodationId", accommodationId.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), UUID::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun findByAttractionId (attractionId: UUID): List<UUID> {
        val querySpec = SqlQuerySpec("SELECT c.id FROM c WHERE c.attractionId = @attractionId", listOf(SqlParameter("@attractionId", attractionId.toString())))
        return container.queryItems(querySpec, CosmosQueryRequestOptions(), UUID::class.java)
            .collectList()
            .awaitSingle()
    }

    override suspend fun findStatusesOfComponents(
        travelOfferId: UUID
    ): Triple<CommuteStatusEnum, AccommodationStatusEnum, AttractionStatusEnum?>? {
        val offerQuery = SqlQuerySpec("SELECT * FROM c WHERE c.id = @id", listOf(SqlParameter("@id", travelOfferId.toString())))
        val offer = container.queryItems(offerQuery, CosmosQueryRequestOptions(), TravelOffer::class.java)
            .singleOrEmpty()
            .awaitSingleOrNull()
        offer ?: return null

        val commuteStatus = offer.commuteId.let { findCommuteById(commuteContainer, it)?.status }
        val accommodationStatus = offer.accommodationId.let { findAccommodationById(accommodationContainer, it)?.status }
        val attractionStatus = offer.attractionId?.let { findAttractionById(attractionContainer, it)?.status }

        return Triple(commuteStatus!!, accommodationStatus!!, attractionStatus)
    }
}
