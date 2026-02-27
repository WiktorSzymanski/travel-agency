package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["pl.szymanski.wiktor.ta.infrastructure.repository.interfaces"])
class MongoRepositoryConfig

