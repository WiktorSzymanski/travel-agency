package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig

object MongoDbProvider {
    private lateinit var dbName: String
    private lateinit var settings: MongoClientSettings

    fun init(config: DatabaseConfig) {
        this.settings =
            MongoClientSettings
                .builder()
                .applyConnectionString(ConnectionString(config.uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        this.dbName = config.dbName
    }

    private val mongoClient: MongoClient by lazy {
        MongoClient.create(settings)
    }

    val database: MongoDatabase by lazy {
        mongoClient.getDatabase(dbName)
    }
}
