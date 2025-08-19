package pl.szymanski.wiktor.ta.commandBootstrap

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.eventHandler.DateMetEventHandler
import pl.szymanski.wiktor.ta.eventHandler.TravelOfferEventHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.config.KurrentDbConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.BookingRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.EventRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.presentation.controller.CommandController
import pl.szymanski.wiktor.ta.presentation.controller.commuteStatisticsController
import pl.szymanski.wiktor.ta.query.CommuteStatisticsQuery
import pl.szymanski.wiktor.ta.service.TravelOfferExpireService
import pl.szymanski.wiktor.ta.service.TravelOfferStatusService

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.application() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))
    KurrentDbProvider.init(property<KurrentDbConfig>("kurrentDatabase"))
    EventBus.init(EventRepositoryImpl())

    val travelOfferRepository = TravelOfferRepositoryImpl(MongoDbProvider.database)
    val accommodationRepository = AccommodationRepositoryImpl(MongoDbProvider.database)
    val attractionRepository = AttractionRepositoryImpl(MongoDbProvider.database)
    val commuteRepository = CommuteRepositoryImpl(MongoDbProvider.database)
    val bookingRepository = BookingRepositoryImpl(MongoDbProvider.database)

    val travelOfferQueryRepository = TravelOfferQueryRepositoryImpl(MongoDbProvider.database)
    val commuteQueryRepository = CommuteQueryRepositoryImpl(MongoDbProvider.database)
    val bookingCommandHandler = BookingCommandHandler(bookingRepository)

    val travelOfferCommandHandler = TravelOfferCommandHandler(travelOfferRepository)
    val travelOfferExpireService = TravelOfferExpireService(travelOfferQueryRepository, travelOfferCommandHandler)
    val travelOfferStatusService = TravelOfferStatusService(travelOfferQueryRepository, travelOfferCommandHandler)


    launch {
        TravelOfferEventHandler(
            travelOfferExpireService = travelOfferExpireService,
            travelOfferStatusService = travelOfferStatusService,
            travelOfferCommandHandler = travelOfferCommandHandler,
            attractionCommandHandler = AttractionCommandHandler(attractionRepository),
            commuteCommandHandler = CommuteCommandHandler(commuteRepository),
            accommodationCommandHandler = AccommodationCommandHandler(accommodationRepository),
            bookingCommandHandler = bookingCommandHandler,
        ).setup()
    }

    launch {
        DateMetEventHandler(
            attractionCommandHandler = AttractionCommandHandler(attractionRepository),
            commuteCommandHandler = CommuteCommandHandler(commuteRepository),
            accommodationCommandHandler = AccommodationCommandHandler(accommodationRepository),
        ).setup()
    }

    CommandController(
        bookingCommandHandler = bookingCommandHandler,
    )

    commuteStatisticsController(
        commuteStatisticsQuery = CommuteStatisticsQuery(commuteQueryRepository),
    )
}
