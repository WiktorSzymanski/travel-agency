package pl.szymanski.wiktor.ta.infrastructure.projection.impl

import io.kurrent.dbclient.ResolvedEvent
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.CommuteId
import pl.szymanski.wiktor.ta.infrastructure.projection.ProjectionHandler
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentCommuteRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import java.util.UUID

@Component
class CommuteProjectionHandler(
    private val kurrentRepository: KurrentCommuteRepository,
    private val queryRepository: CommuteQueryRepository
) : ProjectionHandler {
    override fun canHandle(streamName: String): Boolean = streamName.startsWith("Commute-")

    override suspend fun handle(event: ResolvedEvent, metadata: Metadata) {
        val idStr = event.event.streamId.substringAfter("-")
        val id = CommuteId.from(UUID.fromString(idStr))
        
        val (commute, version) = kurrentRepository.findById(id)
        queryRepository.save(commute)
    }
}
