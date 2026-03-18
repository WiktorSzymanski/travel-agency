package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.infrastructure.document.SagaStateDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.SagaDocumentMongoRepository
import pl.szymanski.wiktor.ta.saga.SagaRepository
import pl.szymanski.wiktor.ta.saga.SagaState
import pl.szymanski.wiktor.ta.saga.SagaStatus
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Component
class SagaRepositoryImpl(
    private val sagaDocumentMongoRepository: SagaDocumentMongoRepository,
) : SagaRepository {

    override suspend fun findById(id: UUID): Pair<SagaState, Long> {
        return withContext(Dispatchers.IO) {
            sagaDocumentMongoRepository.findById(id)
        }.getOrNull()?.let { it.toDomain() to it.version }
            ?: throw NoSuchElementException("Saga not found: $id")
    }

    override suspend fun findByStatuses(statuses: List<SagaStatus>): List<SagaState> {
        return withContext(Dispatchers.IO) {
            sagaDocumentMongoRepository.findByStatusIn(statuses.map { it.name })
        }.map { it.toDomain() }
    }

    override suspend fun save(saga: SagaState) {
        withContext(Dispatchers.IO) {
            sagaDocumentMongoRepository.save(SagaStateDocument.fromDomain(saga, saga.version))
        }
    }

    override suspend fun create(entity: SagaState, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            sagaDocumentMongoRepository.save(SagaStateDocument.fromDomain(entity, metadata.revision))
        }
    }

    override suspend fun save(entity: SagaState, metadata: Metadata) {
        withContext(Dispatchers.IO) {
            sagaDocumentMongoRepository.save(SagaStateDocument.fromDomain(entity, metadata.revision))
        }
    }

    override fun createBlocking(entity: SagaState, metadata: Metadata) {
        sagaDocumentMongoRepository.save(SagaStateDocument.fromDomain(entity, metadata.revision))
    }

    override fun saveBlocking(entity: SagaState, metadata: Metadata) {
        sagaDocumentMongoRepository.save(SagaStateDocument.fromDomain(entity, metadata.revision))
    }
}
