package pl.szymanski.wiktor.ta.infrastructure

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.event.AccommodationCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.AttractionCreatedEvent
import pl.szymanski.wiktor.ta.domain.event.CommuteCreatedEvent
import pl.szymanski.wiktor.ta.offermaker.OfferMakerLogic
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.TravelOfferQueryRepository
import pl.szymanski.wiktor.ta.subscribe

@Service
class OfferMakerImpl(
    private val eventBus: EventBus,
    travelOfferQueryRepository: TravelOfferQueryRepository,
    commuteQueryRepository: CommuteQueryRepository,
    accommodationQueryRepository: AccommodationQueryRepository,
    attractionQueryRepository: AttractionQueryRepository
) : OfferMakerLogic(
    travelOfferQueryRepository, commuteQueryRepository, accommodationQueryRepository, attractionQueryRepository
) {
    suspend fun subscribe() {
        eventBus.subscribe<CommuteCreatedEvent> {
            onCommuteCreatedEvent(it)
        }
        eventBus.subscribe<AccommodationCreatedEvent> {
            onAccommodationCreatedEvent(it)
        }
        eventBus.subscribe<AttractionCreatedEvent> {
            onAttractionCreatedEvent(it)
        }
    }
}

