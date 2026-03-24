package pl.szymanski.wiktor.ta.infrastructure.config

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.KurrentDBClientSettings
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(KurrentDbConfig::class)
class KurrentDBConfiguration {

    @Bean
    fun kurrentDBClient(config: KurrentDbConfig): KurrentDBClient {
        return KurrentDBClient.create(
            KurrentDBClientSettings.builder()
                .addHost(config.host, config.port)
                .defaultCredentials(config.username, config.password)
                .tls(false)
                .keepAliveInterval(30_000)
                .keepAliveTimeout(30_000)
                .defaultDeadline(30_000)
                .buildConnectionSettings()
        )
    }
}

@ConfigurationProperties(prefix = "kurrentdb")
data class KurrentDbConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
)
