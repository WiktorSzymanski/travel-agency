package pl.szymanski.wiktor.ta.infrastructure.repository.interfaces

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import pl.szymanski.wiktor.ta.infrastructure.dto.AccommodationDto

interface AccommodationDtoMongoRepository : MongoRepository<AccommodationDto, String> {
    fun findAccommodationDtosByStatus(status: String, pageable: Pageable): Page<AccommodationDto>

    @Query("{ 'id': ?0 }")
    fun findByUuid(id: String): AccommodationDto?
}
