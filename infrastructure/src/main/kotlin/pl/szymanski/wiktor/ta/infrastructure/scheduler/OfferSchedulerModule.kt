//package pl.szymanski.wiktor.ta.infrastructure.scheduler
//
//import kotlinx.coroutines.launch
//import pl.szymanski.wiktor.ta.commandHandler.TravelOfferCommandHandler
//import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig
//import pl.szymanski.wiktor.ta.infrastructure.repository.MongoDbProvider
//import pl.szymanski.wiktor.ta.infrastructure.repository.command.TravelOfferRepositoryImpl
//import pl.szymanski.wiktor.ta.infrastructure.repository.query.AccommodationQueryRepositoryImpl
//import pl.szymanski.wiktor.ta.infrastructure.repository.query.AttractionQueryRepositoryImpl
//import pl.szymanski.wiktor.ta.infrastructure.repository.query.CommuteQueryRepositoryImpl
//
//fun Application.offerScheduler() {
//    MongoDbProvider.init(property<DatabaseConfig>("database"))
//
//    OfferMakerScheduler.init(
//        property("offerScheduler"),
//        AccommodationQueryRepositoryImpl(MongoDbProvider.database),
//        AttractionQueryRepositoryImpl(MongoDbProvider.database),
//        CommuteQueryRepositoryImpl(MongoDbProvider.database),
//        TravelOfferCommandHandler(TravelOfferRepositoryImpl(MongoDbProvider.database)),
//    )
//
//    launch { OfferMakerScheduler.start() }
//    Runtime.getRuntime().addShutdownHook(Thread { OfferMakerScheduler.stop() })
//}
