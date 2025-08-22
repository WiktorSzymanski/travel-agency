package pl.szymanski.wiktor.ta.infrastructure.repository

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.UuidRepresentation
import pl.szymanski.wiktor.ta.infrastructure.config.DatabaseConfig

//db.dropDatabase() ; db.travelOffer.createIndex( { commuteId: 1, accommodationId: 1, attractionId: 1 },  { unique: true, name: "unique_commute_accommodation_attraction" } ); db.travelOffer.createIndex({ commuteId: 1 }, { name: "idx_commuteId" }); db.travelOffer.createIndex({ accommodationId: 1 }, { name: "idx_accommodationId" }); db.travelOffer.createIndex({ attractionId: 1 }, { name: "idx_attractionId" }); db.travelOffer.createIndex({ status: 1 }, { name: "idx_status" });


object MongoDbProvider {
    private lateinit var dbName: String
    private lateinit var settings: MongoClientSettings

    fun init(config: DatabaseConfig) {
        if (this::settings.isInitialized) return

        this.settings =
            MongoClientSettings
                .builder()
                .applyConnectionString(ConnectionString(config.uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyToConnectionPoolSettings {
                    it.maxSize(200)
                }
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
