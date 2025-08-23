package pl.szymanski.wiktor.ta

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ExceptionHandler")

fun CoroutineScope.launchCatching(block: suspend () -> Unit) = launch {
    runCatching { block() }.onFailure {
        if (it !is IllegalArgumentException) log.error("CoroutineExceptionHandler caught error", it)
//        else log.info(it.message)
    }
}

//val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log.error("CoroutineExceptionHandler catch error", throwable) }