package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.ktor.server.application.Application
import io.ktor.server.config.property
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.command.accommodation.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.command.attraction.AttractionCommandHandler
import pl.szymanski.wiktor.ta.command.commute.CommuteCommandHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider

fun Application.scheduler() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))

    Scheduler.init(
        property("scheduler"),
        AccommodationCommandHandler(AccommodationRepositoryImpl(MongoDbProvider.database)),
        AttractionCommandHandler(AttractionRepositoryImpl(MongoDbProvider.database)),
        CommuteCommandHandler(CommuteRepositoryImpl(MongoDbProvider.database)),
    )

    launch { Scheduler.start() }
    Runtime.getRuntime().addShutdownHook(Thread { Scheduler.stop() })
}
