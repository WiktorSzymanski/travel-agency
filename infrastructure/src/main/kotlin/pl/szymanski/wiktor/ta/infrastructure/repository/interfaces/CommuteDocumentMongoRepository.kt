package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pl.szymanski.wiktor.ta.infrastructure.document.CommuteDocument

@Repository
interface CommuteDocumentMongoRepository : MongoRepository<CommuteDocument, String> {
    fun findCommuteDocumentsByStatus(status: String, pageable: Pageable): Page<CommuteDocument>

    fun findByArrivalLocationAndArrivalTimeBetween(
        location: String,
        from: String,
        to: String
    ): List<CommuteDocument>
}
