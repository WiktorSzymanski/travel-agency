package pl.szymanski.wiktor.ta.infrastructure.config

import kotlinx.serialization.Serializable

@Serializable
class KurrentDbConfig (
    val host : String,
    val port : Int,
    val username : String,
    val password : String,
)