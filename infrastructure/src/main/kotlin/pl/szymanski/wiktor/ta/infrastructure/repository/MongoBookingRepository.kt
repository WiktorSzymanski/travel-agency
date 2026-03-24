package pl.szymanski.wiktor.ta.infrastructure.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import pl.szymanski.wiktor.ta.Metadata
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.domain.BookingState
import pl.szymanski.wiktor.ta.domain.aggregate.Booking
import pl.szymanski.wiktor.ta.domain.aggregate.BookingId
import pl.szymanski.wiktor.ta.repository.CommandRepository
import pl.szymanski.wiktor.ta.infrastructure.document.BookingDocument
import pl.szymanski.wiktor.ta.infrastructure.repository.interfaces.BookingDocumentMongoRepository
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Component
class MongoBookingRepository(
    private val bookingDocumentMongoRepository: BookingDocumentMongoRepository
) : BookingQueryRepository {

    override suspend fun findById(id: BookingId): Pair<Booking, Long> {
        return withContext(Dispatchers.IO) {
            bookingDocumentMongoRepository.findById(id)
        }.getOrNull()?.let { it.toDomain() to it.version }
            ?: throw NoSuchElementException("Booking not found: $id")
    }

    override suspend fun save(entity: Booking) {
        withContext(Dispatchers.IO) {
            bookingDocumentMongoRepository.save(BookingDocument.fromDomain(entity))
        }
    }

    override suspend fun findAllByStatus(status: BookingState, pageable: Pageable): Page<Booking> {
        return withContext(Dispatchers.IO) {
            bookingDocumentMongoRepository.findBookingDocumentsByStatus(status.name, pageable.toSpring())
        }.toApplication { it.toDomain() }
    }

    override suspend fun findAllByUserId(
        userId: UUID,
        pageable: Pageable
    ): Page<Booking> {
        return withContext(Dispatchers.IO) {
            bookingDocumentMongoRepository.findByUserId(userId.toString(), pageable.toSpring())
        }.toApplication { it.toDomain() }
    }
}
