package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.infrastructure.document.CommuteDocument
import pl.szymanski.wiktor.ta.query.CommuteQuery

@RestController
@RequestMapping("/api/commute")
class CommuteController(
    private val commuteQuery: CommuteQuery,
) {

    @GetMapping
    fun getScheduledCommutes(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<CommuteDocument>> = runBlocking {
        val resp = commuteQuery.getScheduledCommutes(Pageable(page, size)).map { CommuteDocument.fromDomain(it) }
        ResponseEntity.ok(resp)
    }
}
