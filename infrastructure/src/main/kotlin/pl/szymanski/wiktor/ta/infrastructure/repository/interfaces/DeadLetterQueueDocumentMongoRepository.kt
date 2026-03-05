package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.document.DeadLetterQueueEntryDocument
import java.util.UUID

@Repository
interface DeadLetterQueueDocumentMongoRepository : MongoRepository<DeadLetterQueueEntryDocument, UUID>

