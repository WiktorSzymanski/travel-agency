package pl.szymanski.wiktor.ta.infrastructure

import io.ktor.server.application.Application
import io.ktor.server.cio.EngineMain
import io.ktor.server.config.property
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.eventHandler.DateMetEventHandler
import pl.szymanski.wiktor.ta.eventHandler.TravelOfferEventHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.controller.travelOfferController
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.application() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))

    val travelOfferRepository = TravelOfferRepositoryImpl(MongoDbProvider.database)
    val accommodationRepository = AccommodationRepositoryImpl(MongoDbProvider.database)
    val attractionRepository = AttractionRepositoryImpl(MongoDbProvider.database)
    val commuteRepository = CommuteRepositoryImpl(MongoDbProvider.database)

    val travelOfferCommandHandler = TravelOfferCommandHandler(travelOfferRepository)
    val travelOfferExpireService = TravelOfferExpireService(travelOfferRepository, travelOfferCommandHandler)

    launch {
        TravelOfferEventHandler(
            travelOfferExpireService = travelOfferExpireService,
            travelOfferCommandHandler = travelOfferCommandHandler,
            attractionCommandHandler = AttractionCommandHandler(attractionRepository),
            commuteCommandHandler = CommuteCommandHandler(commuteRepository),
            accommodationCommandHandler = AccommodationCommandHandler(accommodationRepository),
        ).setup()
    }

    launch {
        DateMetEventHandler(
            attractionCommandHandler = AttractionCommandHandler(attractionRepository),
            commuteCommandHandler = CommuteCommandHandler(commuteRepository),
            accommodationCommandHandler = AccommodationCommandHandler(accommodationRepository),
        ).setup()
    }

    travelOfferController(
        travelOfferRepository = travelOfferRepository,
        commuteRepository = commuteRepository,
        accommodationRepository = accommodationRepository,
        attractionRepository = attractionRepository,
        travelOfferCommandHandler = travelOfferCommandHandler,
    )
}
