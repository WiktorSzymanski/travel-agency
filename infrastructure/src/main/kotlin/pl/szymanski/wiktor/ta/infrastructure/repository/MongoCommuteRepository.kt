package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.infrastructure.document.CommuteDocument
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.CommuteDocumentMongoRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import kotlin.jvm.optionals.getOrNull

@Component
class MongoCommuteRepository(
    private val commuteDocumentMongoRepository: CommuteDocumentMongoRepository
) : CommuteQueryRepository {

    override suspend fun findById(id: CommuteId): Pair<Commute, Long> {
        return withContext(Dispatchers.IO) {
            commuteDocumentMongoRepository.findById(id)
        }.getOrNull()?.let { it.toDomain() to it.version }
            ?: throw NoSuchElementException("Commute not found: $id")
    }

    override suspend fun save(entity: Commute) {
        withContext(Dispatchers.IO) {
            commuteDocumentMongoRepository.save(CommuteDocument.fromDomain(entity))
        }
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: CommuteStatusEnum, pageable: Pageable): Page<Commute> {
        return withContext(Dispatchers.IO) {
            commuteDocumentMongoRepository.findCommuteDocumentsByStatus(status.name, pageable.toSpring())
        }.toApplication { it.toDomain() }
    }

    override suspend fun findByLocationAndArrivalDate(
        location: LocationEnum,
        dateRange: LocalDateTimeRange
    ): List<Commute> {
        return withContext(Dispatchers.IO) {
            commuteDocumentMongoRepository.findByArrivalLocationAndArrivalTimeBetween(
                location.name,
                dateRange.from?.toString() ?: "0000-01-01T00:00:00",
                dateRange.till?.toString() ?: "9999-12-31T23:59:59"
            )
        }.map { it.toDomain() }
    }
}
