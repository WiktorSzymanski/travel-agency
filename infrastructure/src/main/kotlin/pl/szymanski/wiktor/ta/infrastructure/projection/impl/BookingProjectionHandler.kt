package pl.szymanski.wiktor.ta.infrastructure.projection.impl

import io.kurrent.dbclient.ResolvedEvent
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.infrastructure.projection.ProjectionHandler
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentBookingRepository
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID

@Component
class BookingProjectionHandler(
    private val kurrentRepository: KurrentBookingRepository,
    private val queryRepository: BookingQueryRepository
) : ProjectionHandler {
    override fun canHandle(streamName: String): Boolean = streamName.startsWith("Booking-")

    override suspend fun handle(event: ResolvedEvent, metadata: Metadata) {
        val idStr = event.event.streamId.substringAfter("-")
        val id = BookingId.from(UUID.fromString(idStr))
        
        val (booking, version) = kurrentRepository.findById(id)
        queryRepository.save(booking)
    }
}
