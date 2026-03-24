package pl.szymanski.wiktor.ta.infrastructure.projection.impl

import io.kurrent.dbclient.ResolvedEvent
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.infrastructure.projection.ProjectionHandler
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentAttractionRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import java.util.UUID

@Component
class AttractionProjectionHandler(
    private val kurrentRepository: KurrentAttractionRepository,
    private val queryRepository: AttractionQueryRepository
) : ProjectionHandler {
    override fun canHandle(streamName: String): Boolean = streamName.startsWith("Attraction-")

    override suspend fun handle(event: ResolvedEvent, metadata: Metadata) {
        val idStr = event.event.streamId.substringAfter("-")
        val id = AttractionId.from(UUID.fromString(idStr))
        
        val (attraction, version) = kurrentRepository.findById(id)
        queryRepository.save(attraction, Metadata(metadata.correlationId, version))
    }
}
