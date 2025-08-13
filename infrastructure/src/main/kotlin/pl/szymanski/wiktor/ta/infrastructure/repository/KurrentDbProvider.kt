package pl.szymanski.wiktor.ta.infrastructure.repository

import io.kurrent.dbclient.KurrentDBClient
import io.kurrent.dbclient.KurrentDBClientSettings
import pl.szymanski.wiktor.ta.infrastructure.config.KurrentDbConfig

object KurrentDbProvider {
    lateinit var client: KurrentDBClient

    fun init(config: KurrentDbConfig) {
        this.client = KurrentDBClient.create(
            KurrentDBClientSettings.builder()
                .addHost(config.host, config.port)
                .defaultCredentials(config.username, config.password)
                .tls(false)
                .buildConnectionSettings()
        )
    }
}