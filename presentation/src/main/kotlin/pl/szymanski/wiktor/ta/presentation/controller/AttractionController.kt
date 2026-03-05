package pl.szymanski.wiktor.ta.presentation.controller

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.szymanski.wiktor.ta.Page
import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.infrastructure.document.AttractionDocument
import pl.szymanski.wiktor.ta.query.AttractionQuery

@RestController
@RequestMapping("/api/attraction")
class AttractionController(
    private val attractionQuery: AttractionQuery,
) {

    @GetMapping
    fun getScheduledAttractions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AttractionDocument>> = runBlocking {
        val resp = attractionQuery.getScheduledAttractions(Pageable(page, size)).map { AttractionDocument.fromDomain(it)}
        ResponseEntity.ok(resp)
    }
}
