package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.document.OutboxEntryDocument
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface OutboxDocumentMongoRepository : MongoRepository<OutboxEntryDocument, UUID> {
    fun findByPublishedFalseAndProcessAfterLessThanEqualOrderByCreatedAtAsc(
        processAfter: LocalDateTime,
        pageable: Pageable
    ): List<OutboxEntryDocument>
}

