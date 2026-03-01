package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.infrastructure.document.AccommodationDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.AccommodationDocumentMongoRepository
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import pl.szymanski.wiktor.ta.repository.CommandRepository
import kotlin.jvm.optionals.getOrNull

@Component
class MongoAccommodationRepository(
    private val accommodationDocumentMongoRepository: AccommodationDocumentMongoRepository
) : CommandRepository<Accommodation, AccommodationId>, AccommodationQueryRepository {
    override suspend fun create(entity: Accommodation, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            accommodationDocumentMongoRepository.save(AccommodationDocument.fromDomain(entity, metadata.revision))
        }
    }

    override suspend fun save(entity: Accommodation, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            accommodationDocumentMongoRepository.save(AccommodationDocument.fromDomain(entity, metadata.revision))
        }
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented for this approach")
    }

    override suspend fun findById(id: AccommodationId): Pair<Accommodation, Long> {
        return withContext(Dispatchers.IO) {
            accommodationDocumentMongoRepository.findById(id.value)
        }.getOrNull()?.let { it.toDomain() to it.version } ?: throw NoSuchElementException("Accommodation not found: $id")
    }

    override suspend fun findAllByStatus(
        status: AccommodationStatusEnum,
        pageable: Pageable
    ): Page<Accommodation> {
        return withContext(Dispatchers.IO) {
            accommodationDocumentMongoRepository.findAccommodationDocumentsByStatus(
                status.toString(),
                pageable.toSpring()
            )
        }.toApplication { it.toDomain() }
    }
}
