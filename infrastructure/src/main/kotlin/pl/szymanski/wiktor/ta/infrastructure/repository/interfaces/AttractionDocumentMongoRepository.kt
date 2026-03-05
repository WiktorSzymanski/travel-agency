package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.domain.aggregate.AttractionId
import pl.szymanski.wiktor.ta.infrastructure.document.AttractionDocument

@Repository
interface AttractionDocumentMongoRepository : MongoRepository<AttractionDocument, AttractionId> {
    fun findPresentByStatus(status: String, pageable: Pageable): Page<AttractionDocument>
}
