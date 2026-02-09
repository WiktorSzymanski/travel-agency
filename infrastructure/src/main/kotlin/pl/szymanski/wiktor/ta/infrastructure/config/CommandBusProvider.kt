package pl.szymanski.wiktor.ta.infrastructure.config

import pl.szymanski.wiktor.ta.CommandBus
import pl.szymanski.wiktor.ta.infrastructure.StandardCommandBus
import pl.szymanski.wiktor.ta.commandhandler.AccommodationCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.AttractionCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.BookingCommandHandler
import pl.szymanski.wiktor.ta.commandhandler.CommuteCommandHandler
import pl.szymanski.wiktor.ta.domain.repository.AccommodationRepository
import pl.szymanski.wiktor.ta.domain.repository.AttractionRepository
import pl.szymanski.wiktor.ta.domain.repository.BookingRepository
import pl.szymanski.wiktor.ta.domain.repository.CommuteRepository

fun provideAccommodationCommandHandler(
    accommodationRepository: AccommodationRepository
): AccommodationCommandHandler = AccommodationCommandHandler(accommodationRepository)

fun provideAttractionCommandHandler(
    attractionRepository: AttractionRepository
): AttractionCommandHandler = AttractionCommandHandler(attractionRepository)

fun provideBookingCommandHandler(
    bookingRepository: BookingRepository
): BookingCommandHandler = BookingCommandHandler(bookingRepository)

fun provideCommuteCommandHandler(
    commuteRepository: CommuteRepository
): CommuteCommandHandler = CommuteCommandHandler(commuteRepository)

fun provideCommandBus(
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
