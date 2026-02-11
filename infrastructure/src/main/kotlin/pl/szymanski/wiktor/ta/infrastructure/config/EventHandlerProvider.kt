package pl.szymanski.wiktor.ta.infrastructure.config

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.infrastructure.DateMetEventHandler
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository
import pl.szymanski.wiktor.ta.eventHandlerLogic.DateMetEventHandleLogic
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoAccommodationRepository
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoCommuteRepository

fun provideDateMetEventHandleLogic(commandBus: CommandBus): DateMetEventHandleLogic =
    DateMetEventHandleLogic(commandBus)

fun provideDateMetEventHandler(
    eventBus: EventBus,
    accommodationRepository: AccommodationRepository,
    commuteRepository: CommuteRepository,
    attractionRepository: AttractionRepository,
    logic: DateMetEventHandleLogic
): DateMetEventHandler = DateMetEventHandler(
    eventBus, accommodationRepository, commuteRepository, attractionRepository, logic
)
