package pl.szymanski.wiktor.ta.infrastructure.scheduler

import io.ktor.server.application.Application
import io.ktor.server.config.property
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.szymanski.wiktor.ta.commandHandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandHandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AccommodationRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.AttractionRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.command.CommuteRepositoryImpl

fun Application.scheduler() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))

    DataGenerationScheduler.init(
        property("scheduler"),
        AccommodationCommandHandler(AccommodationRepositoryImpl(MongoDbProvider.database)),
        AttractionCommandHandler(AttractionRepositoryImpl(MongoDbProvider.database)),
        CommuteCommandHandler(CommuteRepositoryImpl(MongoDbProvider.database)),
    )

    launch {
        DataGenerationScheduler.start()
        delay(600000)
        DataGenerationScheduler.stop()
    }

    Runtime.getRuntime().addShutdownHook(Thread { DataGenerationScheduler.stop() })
}
