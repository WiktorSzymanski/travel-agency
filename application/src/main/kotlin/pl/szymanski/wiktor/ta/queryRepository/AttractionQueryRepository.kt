package pl.szymanski.wiktor.ta.queryRepository

import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import java.util.UUID

interface AttractionQueryRepository {
    suspend fun save(entity: Attraction): Attraction?

    suspend fun findById(attractionId: UUID): Attraction

    suspend fun findAllByStatus(status: AttractionStatusEnum): List<Attraction>

    suspend fun update(entity: AttractionUpdate)

    suspend fun update(entity: AttractionUpdateStatus)

    suspend fun update(entity: AttractionCancelUpdate)
}

data class AttractionUpdate(
    val _id: UUID,
    val status: AttractionStatusEnum? = null,
    val bookingId: UUID? = null
)

data class AttractionCancelUpdate(
    val _id: UUID,
    val status: AttractionStatusEnum? = null,
    val bookingId: UUID? = null
)

data class AttractionUpdateStatus(
    val _id: UUID,
    val status: AttractionStatusEnum? = null,
)