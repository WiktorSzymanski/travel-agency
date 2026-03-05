package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.document.SagaStateDocument
import java.util.UUID

@Repository
interface SagaDocumentMongoRepository : MongoRepository<SagaStateDocument, UUID> {
    fun findByStatusIn(statuses: List<String>): List<SagaStateDocument>
}

