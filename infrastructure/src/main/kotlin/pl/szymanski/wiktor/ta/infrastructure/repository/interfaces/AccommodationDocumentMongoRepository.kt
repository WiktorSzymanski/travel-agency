package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.infrastructure.document.AccommodationDocument

@Repository
interface AccommodationDocumentMongoRepository : MongoRepository<AccommodationDocument, AccommodationId> {
    fun findAccommodationDocumentsByStatus(status: String, pageable: Pageable): Page<AccommodationDocument>
}
