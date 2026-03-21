//package pl.szymanski.wiktor.ta
//
//import pl.szymanski.wiktor.ta.commands.accommodation.AccommodationCommand
//import pl.szymanski.wiktor.ta.commands.attraction.AttractionCommand
//import pl.szymanski.wiktor.ta.commands.attraction.CompensateAttractionCommand
//import pl.szymanski.wiktor.ta.commands.booking.BookingCommand
//import pl.szymanski.wiktor.ta.commands.Command
//import pl.szymanski.wiktor.ta.commands.commute.CommuteCommand
//import pl.szymanski.wiktor.ta.commands.commute.CompensateCommuteCommand
//import pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.create.CreateAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommandHandler
//import pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.create.CreateAttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.compensateBook.CompensateBookAttractionCommandHandler
//import pl.szymanski.wiktor.ta.commands.attraction.compensateCancelBooking.CompensateCancelAttractionBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.create.CreateCommuteCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommandHandler
//import pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommandHandler
//import pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommandHandler
//
//class DummyCommandBus : CommandBus {
//    private val handlers = mutableMapOf<Class<out Command>, CommandHandler<*, *>>()
//
//    override fun <C : Command, E> registerHandler(commandType: Class<C>, handler: CommandHandler<C, E>) {
//        handlers[commandType] = handler
//    }
//
//    override suspend fun <C : Command, E> dispatch(command: C) {
//        val handler =
//            handlers[command::class.java.superclass] as? CommandHandler<C, E>
//                ?: throw IllegalArgumentException("No handler registered for ${command::class.java.superclass}")
//        return handler.handle(command)
//    }
//
//    override suspend fun dispatchAndForget(command: Command) {
//        val handler =
//            handlers[command::class.java.superclass] as? CommandHandler<Command, Any>
//                ?: throw IllegalArgumentException("No handler registered for ${command::class.java.superclass}")
//        handler.handle(command)
//    }
//
//    constructor()
//
//    constructor(
//        createAccommodationCommandHandler: CreateAccommodationCommandHandler,
//        bookAccommodationCommandHandler: BookAccommodationCommandHandler,
//        cancelAccommodationBookingCommandHandler: CancelAccommodationBookingCommandHandler,
//        expireAccommodationCommandHandler: ExpireAccommodationCommandHandler,
//        compensateBookAccommodationCommandHandler: CompensateBookAccommodationCommandHandler,
//        compensateCancelAccommodationBookingCommandHandler: CompensateCancelAccommodationBookingCommandHandler,
//        createAttractionCommandHandler: CreateAttractionCommandHandler,
//        bookAttractionCommandHandler: BookAttractionCommandHandler,
//        cancelAttractionBookingCommandHandler: CancelAttractionBookingCommandHandler,
//        expireAttractionCommandHandler: ExpireAttractionCommandHandler,
//        compensateBookAttractionCommandHandler: CompensateBookAttractionCommandHandler,
//        compensateCancelAttractionBookingCommandHandler: CompensateCancelAttractionBookingCommandHandler,
//        createCommuteCommandHandler: CreateCommuteCommandHandler,
//        bookCommuteCommandHandler: BookCommuteCommandHandler,
//        cancelCommuteBookingCommandHandler: CancelCommuteBookingCommandHandler,
//        expireCommuteCommandHandler: ExpireCommuteCommandHandler,
//        compensateBookCommuteCommandHandler: CompensateBookCommuteCommandHandler,
//        compensateCancelCommuteBookingCommandHandler: CompensateCancelCommuteBookingCommandHandler,
//        createBookingCommandHandler: CreateBookingCommandHandler,
//        processBookingCommandHandler: ProcessBookingCommandHandler,
//        completeBookingCommandHandler: CompleteBookingCommandHandler,
//        cancelBookingCommandHandler: CancelBookingCommandHandler,
//        failBookingCommandHandler: FailBookingCommandHandler,
//        failCancelBookingCommandHandler: FailCancelBookingCommandHandler,
//        processCancelBookingCommandHandler: ProcessCancelBookingCommandHandler,
//        bookingRequestCancelCommandHandler: BookingRequestCancelCommandHandler,
//    ) {
//        this.registerHandler(AccommodationCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.accommodation.create.CreateAccommodationCommand -> createAccommodationCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.accommodation.book.BookAccommodationCommand -> bookAccommodationCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.accommodation.cancelBooking.CancelAccommodationBookingCommand -> cancelAccommodationBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.accommodation.expire.ExpireAccommodationCommand -> expireAccommodationCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.accommodation.compensateBook.CompensateBookAccommodationCommand -> compensateBookAccommodationCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.accommodation.compensateCancelBooking.CompensateCancelAccommodationBookingCommand -> compensateCancelAccommodationBookingCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//        this.registerHandler(AttractionCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.attraction.create.CreateAttractionCommand -> createAttractionCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.book.BookAttractionCommand -> bookAttractionCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.cancelBooking.CancelAttractionBookingCommand -> cancelAttractionBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.expire.ExpireAttractionCommand -> expireAttractionCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.compensateBook.CompensateBookAttractionCommand -> compensateBookAttractionCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.compensateCancelBooking.CompensateCancelAttractionBookingCommand -> compensateCancelAttractionBookingCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//        this.registerHandler(CompensateAttractionCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.attraction.compensateBook.CompensateBookAttractionCommand -> compensateBookAttractionCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.attraction.compensateCancelBooking.CompensateCancelAttractionBookingCommand -> compensateCancelAttractionBookingCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//        this.registerHandler(CommuteCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.commute.create.CreateCommuteCommand -> createCommuteCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.book.BookCommuteCommand -> bookCommuteCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.cancelBooking.CancelCommuteBookingCommand -> cancelCommuteBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.expire.ExpireCommuteCommand -> expireCommuteCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommand -> compensateBookCommuteCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommand -> compensateCancelCommuteBookingCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//        this.registerHandler(CompensateCommuteCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.commute.compensateBook.CompensateBookCommuteCommand -> compensateBookCommuteCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.commute.compensateCancelBooking.CompensateCancelCommuteBookingCommand -> compensateCancelCommuteBookingCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//        this.registerHandler(BookingCommand::class.java) { command ->
//            when (command) {
//                is pl.szymanski.wiktor.ta.commands.booking.create.CreateBookingCommand -> createBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.process.ProcessBookingCommand -> processBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.complete.CompleteBookingCommand -> completeBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.cancel.CancelBookingCommand -> cancelBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.fail.FailBookingCommand -> failBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.failCancel.FailCancelBookingCommand -> failCancelBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.processCancel.ProcessCancelBookingCommand -> processCancelBookingCommandHandler.handle(command)
//                is pl.szymanski.wiktor.ta.commands.booking.requestCancel.BookingRequestCancelCommand -> bookingRequestCancelCommandHandler.handle(command)
//                else -> throw IllegalArgumentException("No handler for ${command::class.java.name}")
//            }
//        }
//    }
//}