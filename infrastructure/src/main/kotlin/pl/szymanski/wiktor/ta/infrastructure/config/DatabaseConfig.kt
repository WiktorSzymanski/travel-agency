package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val uri: String,
    val dbName: String,
)
