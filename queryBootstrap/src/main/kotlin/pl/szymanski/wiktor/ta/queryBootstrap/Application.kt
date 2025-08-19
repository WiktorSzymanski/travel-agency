package pl.szymanski.wiktor.ta.queryBootstrap

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import pl.szymanski.wiktor.ta.EventBus
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.config.KurrentDbConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.command.EventRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.BookingQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.presentation.controller.QueryController
import pl.szymanski.wiktor.ta.presentation.controller.commuteStatisticsController
import pl.szymanski.wiktor.ta.query.BookingQuery
import pl.szymanski.wiktor.ta.query.CommuteStatisticsQuery

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.application() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))
    KurrentDbProvider.init(property<KurrentDbConfig>("kurrentDatabase"))
    EventBus.init(EventRepositoryImpl())

    val travelOfferQueryRepository = TravelOfferQueryRepositoryImpl(MongoDbProvider.database)
    val bookingQueryRepository = BookingQueryRepositoryImpl(MongoDbProvider.database)
    val accommodationQueryRepository = AccommodationQueryRepositoryImpl(MongoDbProvider.database)
    val commuteQueryRepository = CommuteQueryRepositoryImpl(MongoDbProvider.database)
    val bookingQuery = BookingQuery(bookingQueryRepository)

    QueryController(
        travelOfferQueryRepository = travelOfferQueryRepository,
        accommodationQueryRepository = accommodationQueryRepository,
        bookingQuery = bookingQuery
    )

    commuteStatisticsController(
        commuteStatisticsQuery = CommuteStatisticsQuery(commuteQueryRepository),
    )
}
