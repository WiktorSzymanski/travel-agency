package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class MongoDelayedEventRepository(
    mongoConfiguration: MongoConfiguration
) : DelayedEventRepository {
    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<DelayedEvent>("delayed_events")

    override suspend fun save(event: DelayedEvent) {
        collection.insertOne(event)
    }

    override suspend fun findReadyToPublish(now: LocalDateTime): List<DelayedEvent> {
        val nowStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return collection.find(
            Filters.and(
                Filters.eq("processed", false),
                Filters.lte("scheduledAt", nowStr)
            )
        ).toList()
    }

    override suspend fun markAsProcessed(id: String) {
        collection.updateOne(
            Filters.eq("id", id),
            Updates.set("processed", true)
        )
    }
}
