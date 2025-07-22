package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.ktor.server.application.Application
import io.ktor.server.config.property
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.TravelOfferRepositoryImpl

fun Application.offerScheduler() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))

    OfferMakerScheduler.init(
        property("offerScheduler"),
        AccommodationRepositoryImpl(MongoDbProvider.database),
        AttractionRepositoryImpl(MongoDbProvider.database),
        CommuteRepositoryImpl(MongoDbProvider.database),
        TravelOfferCommandHandler(TravelOfferRepositoryImpl(MongoDbProvider.database)),
    )

    launch { OfferMakerScheduler.start() }
    Runtime.getRuntime().addShutdownHook(Thread { OfferMakerScheduler.stop() })
}
