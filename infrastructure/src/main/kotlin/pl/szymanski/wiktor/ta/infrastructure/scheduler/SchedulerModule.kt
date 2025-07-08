package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.ktor.server.application.Application
import io.ktor.server.config.property
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.CommuteRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider

fun Application.scheduler() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))

    Scheduler.init(
        property("scheduler"),
        AccommodationRepositoryImpl(MongoDbProvider.database),
        AttractionRepositoryImpl(MongoDbProvider.database),
        CommuteRepositoryImpl(MongoDbProvider.database),
    )

    Scheduler.start()
    Runtime.getRuntime().addShutdownHook(Thread { Scheduler.stop() })
}
