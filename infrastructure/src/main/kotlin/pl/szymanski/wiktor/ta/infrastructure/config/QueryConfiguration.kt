package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.query.AccommodationQuery
import pl.szymanski.wiktor.ta.query.AttractionQuery
import pl.szymanski.wiktor.ta.query.BookingQuery
import pl.szymanski.wiktor.ta.query.CommuteQuery
import pl.szymanski.wiktor.ta.queryrepository.BookingQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AccommodationQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.AttractionQueryRepository
import pl.szymanski.wiktor.ta.queryrepository.CommuteQueryRepository

@Configuration
class QueryConfiguration {
    @Bean
    fun commuteQuery(commuteRepository: CommuteQueryRepository): CommuteQuery = CommuteQuery(commuteRepository)

    @Bean
    fun accommodationQuery(accommodationRepository: AccommodationQueryRepository): AccommodationQuery =
        AccommodationQuery(accommodationRepository)

    @Bean
    fun attractionQuery(attractionRepository: AttractionQueryRepository): AttractionQuery =
        AttractionQuery(attractionRepository)

    @Bean
    fun bookingQuery(bookingQueryRepository: BookingQueryRepository): BookingQuery =
        BookingQuery(bookingQueryRepository)
}