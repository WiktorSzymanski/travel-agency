package pl.szymanski.wiktor.ta

import kotlinx.coroutines.delay
import pl.szymanski.wiktor.ta.domain.aggregate.Accommodation
import pl.szymanski.wiktor.ta.domain.aggregate.Attraction
import pl.szymanski.wiktor.ta.domain.aggregate.Commute
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.reflect.KClass

fun Attraction.timeMet(): Boolean = this.date.isBefore(LocalDateTime.now())

fun Accommodation.timeMet(): Boolean = this.rent.from.isBefore(LocalDateTime.now())

fun Commute.timeMet(): Boolean = this.departure.time.isBefore(LocalDateTime.now())

private const val ATTEMPTS_LOG_THRESHOLD = 25

suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 10_000,
    backoffMultiplier: Double = 2.0,
    jitterFactor: Double = 0.1,
    onException: KClass<out Exception> = ConcurrentModificationException::class,
    action: suspend () -> T,
): T {
    var lastException: Throwable? = null
    var currentDelay = initialDelayMs

    repeat(maxRetries) { attempt ->
        val result = runCatching { action() }
        if (result.isSuccess) return result.getOrThrow()

        lastException = result.exceptionOrNull()
        if (!onException.isInstance(lastException)) throw lastException!!

            // Log after a number of failed attempts to avoid noisy logs on transient errors
        if (attempt > ATTEMPTS_LOG_THRESHOLD) {
            println("ERROR: ${lastException!!.message} \t Retrying attempt: $attempt")
        }

        if (attempt < maxRetries - 1) {
            val jitter = currentDelay * jitterFactor * Random.nextDouble()
            val delayWithJitter = currentDelay + jitter
            delay(delayWithJitter.toLong())

            currentDelay = (currentDelay * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
        }
    }
    // When all retries are exhausted, rethrow the last exception if present, otherwise fail deliberately
    lastException?.let { throw it }
    error("No attempt made")
}