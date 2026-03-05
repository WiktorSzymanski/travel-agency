package pl.szymanski.wiktor.ta.infrastructure.outbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueEntry
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.infrastructure.document.DeadLetterQueueEntryDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.DeadLetterQueueDocumentMongoRepository

@Repository
class DeadLetterQueueRepositoryMongo(
    private val deadLetterQueueDocumentMongoRepository: DeadLetterQueueDocumentMongoRepository,
) : DeadLetterQueueRepository {

    override suspend fun save(dlqEntry: DeadLetterQueueEntry) {
        withContext(Dispatchers.IO) {
            deadLetterQueueDocumentMongoRepository.save(DeadLetterQueueEntryDocument.fromDomain(dlqEntry))
        }
    }
}