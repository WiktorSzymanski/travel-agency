package pl.szymanski.wiktor.ta.infrastructure.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.server.plugins.di.annotations.Property
import kotlinx.serialization.Serializable
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider

class DatabaseProvider(
    @Property("database") val mongoConfig: MongoDBConfig
) {
    val mongoClient: MongoClient by lazy {
        val codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(KotlinSerializerCodecProvider())
        )
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(mongoConfig.uri))
            .codecRegistry(codecRegistry)
            .build()
        MongoClient.create(settings)
    }
}

@Serializable
class MongoDBConfig (
    val uri: String,
    val dbName: String,
) {
    companion object {
    }
}