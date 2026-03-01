package pl.szymanski.wiktor.ta.infrastructure.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.util.*

@ConfigurationProperties(prefix = "database")
data class MongoDBConfig(
    var uri: String = "",
    var dbName: String = ""
)

@Configuration
class MongoConfiguration(
    val mongoConfig: MongoDBConfig
) {
    @Bean
    fun mongoClient(): MongoClient {
        val codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(KotlinSerializerCodecProvider())
        )
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(mongoConfig.uri))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(codecRegistry)
            .build()
        return MongoClient.create(settings)
    }
}