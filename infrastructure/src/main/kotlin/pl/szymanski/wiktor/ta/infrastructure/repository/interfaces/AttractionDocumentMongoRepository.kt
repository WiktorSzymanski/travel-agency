package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.document.AttractionDocument
import java.util.UUID

@Repository
interface AttractionDocumentMongoRepository : MongoRepository<AttractionDocument, UUID> {
    fun findPresentByStatus(status: String, pageable: Pageable): Page<AttractionDocument>
}
