package pl.szymanski.wiktor.ta.infrastructure

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
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
    attractionQueryRepository: AttractionQueryRepository,
    @Value("\${generator.creation-window-seconds:3}")
    creationWindowSeconds: Long
) : OfferMakerLogic(
    travelOfferQueryRepository, commuteQueryRepository, accommodationQueryRepository, attractionQueryRepository, creationWindowSeconds
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PostConstruct
    fun init() {
        scope.launch {
            eventBus.subscribe<CommuteCreatedEvent> {
                onCommuteCreatedEvent(it)
            }
        }

        scope.launch {
            eventBus.subscribe<AccommodationCreatedEvent> {
                onAccommodationCreatedEvent(it)
            }
        }

        scope.launch {
            eventBus.subscribe<AttractionCreatedEvent> {
                onAttractionCreatedEvent(it)
            }
        }
    }
}

