package pl.szymanski.wiktor.ta.domain.repository

import pl.szymanski.wiktor.ta.domain.AccommodationStatusEnum
import pl.szymanski.wiktor.ta.domain.AttractionStatusEnum
import pl.szymanski.wiktor.ta.domain.CommuteStatusEnum
import pl.szymanski.wiktor.ta.domain.aggregate.TravelOffer
import pl.szymanski.wiktor.ta.domain.event.TravelOfferEvent
import java.util.UUID

interface TravelOfferRepository {
    suspend fun findById(travelOfferId: UUID): TravelOffer
}
