package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import pl.szymanski.wiktor.ta.infrastructure.config.converter.*

@Configuration
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = ["pl.szymanski.wiktor.ta.infrastructure.repository.interfaces"])
class MongoRepositoryConfig {

    @Bean
    fun transactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager {
        return MongoTransactionManager(dbFactory)
    }

    @Bean
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                CommuteIdToUUIDConverter(),
                UUIDToCommuteIdConverter(),
                AccommodationIdToUUIDConverter(),
                UUIDToAccommodationIdConverter(),
                AttractionIdToUUIDConverter(),
                UUIDToAttractionIdConverter(),
                BookingIdToUUIDConverter(),
                UUIDToBookingIdConverter(),
            )
        )
    }
}
