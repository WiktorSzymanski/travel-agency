package pl.szymanski.wiktor.ta.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.cio.EngineMain
import io.ktor.server.config.property
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.presentation.controller.travelOfferController
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.eventHandler.DateMetEventHandler
import pl.szymanski.wiktor.ta.eventHandler.TravelOfferEventHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.presentation.controller.commuteStatisticsController
import pl.szymanski.wiktor.ta.query.CommuteStatisticsQuery
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

    val travelOfferQueryRepository = TravelOfferQueryRepositoryImpl(MongoDbProvider.database)
    val accommodationQueryRepository = AccommodationQueryRepositoryImpl(MongoDbProvider.database)
    val commuteQueryRepository = CommuteQueryRepositoryImpl(MongoDbProvider.database)

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
        travelOfferQueryRepository = travelOfferQueryRepository,
        travelOfferCommandHandler = travelOfferCommandHandler,
        accommodationQueryRepository = accommodationQueryRepository,
    )

    commuteStatisticsController(
        commuteStatisticsQuery = CommuteStatisticsQuery(commuteQueryRepository),
    )
}
