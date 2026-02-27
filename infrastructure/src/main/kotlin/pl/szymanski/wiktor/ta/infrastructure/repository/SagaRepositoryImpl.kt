package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.infrastructure.config.MongoConfiguration
import pl.szymanski.wiktor.ta.saga.SagaRepository
import pl.szymanski.wiktor.ta.saga.SagaState
import pl.szymanski.wiktor.ta.saga.SagaStatus
import java.util.UUID

@Repository
class SagaRepositoryImpl(
    mongoConfiguration: MongoConfiguration
) : SagaRepository {

    private val collection = mongoConfiguration.mongoClient()
        .getDatabase(mongoConfiguration.mongoConfig.dbName)
        .getCollection<SagaState>("sagas")

    override suspend fun findById(id: UUID): Pair<SagaState, Long> {
        return collection
            .find(Filters.eq("_id", id))
            .toList()
            .first()
            .let { it to it.version }
    }

    override suspend fun findByStatuses(statuses: List<SagaStatus>): List<SagaState> {
        return collection
            .find(Filters.`in`("status", statuses.map { it.name }))
            .toList()
    }

    override suspend fun save(saga: SagaState) {
        collection.replaceOne(
            Filters.eq("_id", saga.id),
            saga,
            ReplaceOptions().upsert(true)
        )
    }

    override suspend fun create(
        entity: SagaState,
        metadata: Metadata
    ) {
        collection.replaceOne(
            Filters.eq("_id", entity.id),
            entity,
            ReplaceOptions().upsert(true)
        )
    }

    override suspend fun save(
        entity: SagaState,
        metadata: Metadata
    ) {
        collection.replaceOne(
            Filters.eq("_id", entity.id),
            entity,
            ReplaceOptions().upsert(true)
        )
    }

}
