package pl.szymanski.wiktor.ta.infrastructure.repository

import pl.szymanski.wiktor.ta.Pageable
import pl.szymanski.wiktor.ta.Sort
import pl.szymanski.wiktor.ta.Page

import org.springframework.data.domain.Sort as SpringSort
import org.springframework.data.domain.Pageable as SpringPageable
import org.springframework.data.domain.Page as SpringPage
import org.springframework.data.domain.PageRequest

fun Sort.Direction.toSpring(): SpringSort.Direction = when (this) {
    Sort.Direction.ASC -> SpringSort.Direction.ASC
    Sort.Direction.DESC -> SpringSort.Direction.DESC
}

fun Sort.toSpring(): SpringSort {
    return SpringSort.by(this.orders.map { SpringSort.Order(it.direction.toSpring(), it.property) })
}

fun Pageable.toSpring(): SpringPageable {
    return PageRequest.of(this.page, this.size, this.sort.toSpring())
}

fun <T : Any, R> SpringPage<T>.toApplication(mapper: (T) -> R): Page<R> {
    return Page(
        content = this.content.map(mapper),
        page = this.number,
        size = this.size,
        totalElements = this.totalElements
    )
}