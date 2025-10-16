package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import java.util.UUID

interface TravelOfferRepository {
    suspend fun findById(id: UUID): TravelOffer

    suspend fun create(
        entity: TravelOffer,
        event: TravelOfferEvent,
    )

    suspend fun save(
        entity: TravelOffer,
        event: TravelOfferEvent,
    )
}
