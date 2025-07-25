package pl.szymanski.wiktor.ta.presentation.controller

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import pl.szymanski.wiktor.ta.query.CommuteStatisticsQuery
import java.time.LocalDateTime

fun Application.commuteStatisticsController(
    commuteStatisticsQuery: CommuteStatisticsQuery
) {
    install(AsyncApiPlugin) {
        extension =
            AsyncApiExtension.builder {
                info {
                    title("Sample API")
                    version("1.0.0")
                }
            }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    fun extractDataRanges(queryParams: Parameters): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()

        val start = queryParams["start"]?.let { LocalDateTime.parse(it) } ?: now.minusHours(1)
        val end = queryParams["end"]?.let { LocalDateTime.parse(it) } ?: now

        return Pair(start, end)
    }

    fun extractPaginationParams(queryParams: Parameters): Pair<Int, Int> {
        val page = queryParams["page"]?.toIntOrNull() ?: 1
        val size = queryParams["size"]?.toIntOrNull() ?: 20

        return Pair(page, size)
    }

    routing {
        get("/commuteStatistics") {
            val (start, end) = extractDataRanges(call.request.queryParameters)
            val (page, size) = extractPaginationParams(call.request.queryParameters)

            val resp = commuteStatisticsQuery.getCommuteStats(page, size, start, end)
            call.response.status(HttpStatusCode.OK)
            call.respond(resp)
        }
    }
}