package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
class MongoDBConfig (
    val uri: String,
    val dbName: String,
)