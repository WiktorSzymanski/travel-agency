package pl.szymanski.wiktor.ta.infrastructure.repository.command

import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay

private val log = org.slf4j.LoggerFactory.getLogger("retryOnUnavailable")

suspend fun <T> retryOnUnavailable(
    maxRetries: Int = 3,
    delayMillis: Long = 1000,
    block: suspend () -> T
): T {
    var attempt = 0
    while (true) {
        try {
            return block()
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.UNAVAILABLE && attempt < maxRetries) {
                attempt++
                log.warn("Call failed with ${e.status.code}, retrying $attempt/$maxRetries")
                delay(delayMillis)
            } else {
                throw e
            }
        }
    }
}

