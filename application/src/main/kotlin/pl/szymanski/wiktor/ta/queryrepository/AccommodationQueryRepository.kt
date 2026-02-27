package pl.szymanski.wiktor.ta.queryrepository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.LocationEnum
import pl.szymanski.wiktor.ta.domain.TravelOfferStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.AccommodationId
import pl.szymanski.wiktor.ta.LocalDateTimeRange
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import java.time.LocalDateTime

interface AccommodationQueryRepository {
    suspend fun save(entity: Accommodation)

    suspend fun update(projectionUpdate: ProjectionUpdate)

    suspend fun findById(id: AccommodationId): Pair<Accommodation, Long>

    suspend fun findAllByStatus(status: AccommodationStatusEnum, pageable: Pageable): Page<Accommodation>

    suspend fun findByLocationAndDate(location: LocationEnum, dateRange: LocalDateTimeRange): List<Accommodation>

    suspend fun findByLocationAndRentContainsDate(location: LocationEnum, date: LocalDateTime): List<Accommodation>

    suspend fun countTravelOfferByLocation(
        location: LocationEnum,
        status: TravelOfferStatusEnum? = null,
    ): Int
}
