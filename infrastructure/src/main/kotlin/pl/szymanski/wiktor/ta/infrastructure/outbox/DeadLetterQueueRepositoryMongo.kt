package pl.szymanski.wiktor.ta.infrastructure.outbox

import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueEntry
import pl.szymanski.wiktor.ta.dlq.DeadLetterQueueRepository
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration

@Repository
class DeadLetterQueueRepositoryMongo(
    mongoConfiguration: MongoConfiguration
) : DeadLetterQueueRepository {

    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<DeadLetterQueueEntry>("dead_letter_queue")

    override suspend fun save(dlqEntry: DeadLetterQueueEntry) {
        collection.insertOne(dlqEntry)
    }
}