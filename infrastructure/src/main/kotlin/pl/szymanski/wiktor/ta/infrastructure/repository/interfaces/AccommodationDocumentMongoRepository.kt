package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import pl.szymanski.wiktor.ta.infrastructure.document.AccommodationDocument

interface AccommodationDocumentMongoRepository : MongoRepository<AccommodationDocument, String> {
    fun findAccommodationDocumentsByStatus(status: String, pageable: Pageable): Page<AccommodationDocument>

    @Query("{ 'id': ?0 }")
    fun findByUuid(id: String): AccommodationDocument?
}

