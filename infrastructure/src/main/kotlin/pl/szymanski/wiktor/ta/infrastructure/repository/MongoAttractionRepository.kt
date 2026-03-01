package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.infrastructure.document.AttractionDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.AttractionDocumentMongoRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.ProjectionUpdate
import pl.szymanski.wiktor.ta.repository.CommandRepository
import kotlin.jvm.optionals.getOrNull

@Component
class MongoAttractionRepository(
    private val attractionDocumentMongoRepository: AttractionDocumentMongoRepository,
) : CommandRepository<Attraction, AttractionId>, AttractionQueryRepository {

    override suspend fun findById(id: AttractionId): Pair<Attraction, Long> {
        val uuid = id.value ?: throw IllegalArgumentException("AttractionId value cannot be null")
        return withContext(Dispatchers.IO) {
            attractionDocumentMongoRepository.findById(uuid)
        }.getOrNull()?.let { it.toDomain() to it.version } ?: throw NoSuchElementException("Attraction not found: $id")
    }

    override suspend fun create(entity: Attraction, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            attractionDocumentMongoRepository.save(AttractionDocument.fromDomain(entity, metadata.revision))
        }
    }

    override suspend fun save(entity: Attraction, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            attractionDocumentMongoRepository.save(AttractionDocument.fromDomain(entity, metadata.revision))
        }
    }

    override suspend fun update(projectionUpdate: ProjectionUpdate) {
        throw UnsupportedOperationException("Not implemented in this approach")
    }

    override suspend fun findAllByStatus(status: AttractionStatusEnum, pageable: Pageable): Page<Attraction> {
        return withContext(Dispatchers.IO) {
            attractionDocumentMongoRepository.findPresentByStatus(status.toString(), pageable.toSpring())
        }.toApplication { it.toDomain() }
    }
}
