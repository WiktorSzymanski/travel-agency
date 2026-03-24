package pl.szymanski.wiktor.ta.infrastructure.projection.impl

import io.kurrent.dbclient.ResolvedEvent
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.infrastructure.projection.ProjectionHandler
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentAccommodationRepository
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import java.util.UUID

@Component
class AccommodationProjectionHandler(
    private val kurrentRepository: KurrentAccommodationRepository,
    private val queryRepository: AccommodationQueryRepository
) : ProjectionHandler {
    override fun canHandle(streamName: String): Boolean = streamName.startsWith("Accommodation-")

    override suspend fun handle(event: ResolvedEvent, metadata: Metadata) {
        val idStr = event.event.streamId.substringAfter("-")
        val id = AccommodationId.from(UUID.fromString(idStr))
        
        val (accommodation, version) = kurrentRepository.findById(id)
        queryRepository.save(accommodation, Metadata(metadata.correlationId, version))
    }
}
