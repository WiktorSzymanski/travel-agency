package pl.szymanski.wiktor.ta.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.infrastructure.StandardCommandBus
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.repository.AttractionRepository
import pl.szymanski.wiktor.ta.repository.BookingRepository
import pl.szymanski.wiktor.ta.repository.CommuteRepository

@Configuration
class CommandBusConfiguration {

    @Bean
    fun accommodationCommandHandler(
        accommodationRepository: AccommodationRepository
    ): AccommodationCommandHandler = AccommodationCommandHandler(accommodationRepository)

    @Bean
    fun attractionCommandHandler(
        attractionRepository: AttractionRepository
    ): AttractionCommandHandler = AttractionCommandHandler(attractionRepository)

    @Bean
    fun bookingCommandHandler(
        bookingRepository: BookingRepository
    ): BookingCommandHandler = BookingCommandHandler(bookingRepository)

    @Bean
    fun commuteCommandHandler(
        commuteRepository: CommuteRepository
    ): CommuteCommandHandler = CommuteCommandHandler(commuteRepository)

    @Bean
    fun commandBus(
        bookingCommandHandler: BookingCommandHandler,
        commuteCommandHandler: CommuteCommandHandler,
        attractionCommandHandler: AttractionCommandHandler,
        accommodationCommandHandler: AccommodationCommandHandler,
    ): CommandBus = StandardCommandBus(
        bookingCommandHandler,
        commuteCommandHandler,
        attractionCommandHandler,
        accommodationCommandHandler
    )
}
