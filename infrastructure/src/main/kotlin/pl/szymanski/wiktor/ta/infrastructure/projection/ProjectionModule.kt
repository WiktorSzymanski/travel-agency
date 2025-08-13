package pl.szymanski.wiktor.ta.infrastructure.projection

import io.ktor.server.application.Application
import io.ktor.server.config.property
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
import pl.szymanski.wiktor.ta.infrastructure.config.KurrentDbConfig
import pl.szymanski.wiktor.ta.infrastructure.repository.KurrentDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.AttractionQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.TravelOfferQueryRepositoryImpl
import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl

fun Application.projectionModule() {
    MongoDbProvider.init(property<DatabaseConfig>("database"))
    KurrentDbProvider.init(property<KurrentDbConfig>("kurrentDatabase"))

    val accommodationQueryRepository = AccommodationQueryRepositoryImpl(MongoDbProvider.database)
    val travelOfferQueryRepository = TravelOfferQueryRepositoryImpl(MongoDbProvider.database)
    val commuteQueryRepository = CommuteQueryRepositoryImpl(MongoDbProvider.database)
    val attractionQueryRepository = AttractionQueryRepositoryImpl(MongoDbProvider.database)

    // Start projection services
    AccommodationProjectionService(KurrentDbProvider.client, accommodationQueryRepository).startProjection()
    TravelOfferProjectionService(KurrentDbProvider.client, travelOfferQueryRepository).startProjection()
    CommuteProjectionService(KurrentDbProvider.client, commuteQueryRepository).startProjection()
    AttractionProjectionService(KurrentDbProvider.client, attractionQueryRepository).startProjection()
}