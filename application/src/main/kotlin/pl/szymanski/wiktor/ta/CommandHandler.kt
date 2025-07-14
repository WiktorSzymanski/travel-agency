//package pl.szymanski.wiktor.ta
//
//import pl.szymanski.wiktor.ta.service.TravelOfferService
//
//class CommandHandler(
//    private val travelOfferService: TravelOfferService,
//) {
////    suspend fun handle(command: TravelOfferCommand) {
////        when (command) {
////            is BookTravelOfferCommand -> handle(command)
////            is CancelBookTravelOfferCommand -> handle(command)
////        }
////    }
//
////    suspend fun handle(command: BookTravelOfferCommand) {
////        travelOfferService.bookTravelOffer(
////            command.travelOfferId,
////            command.userId,
////            command.seat,
////        )
////    }
////
////    suspend fun handle(command: CancelBookTravelOfferCommand) {
////        travelOfferService.cancelTravelOfferBooking(
////            command.travelOfferId,
////            command.userId,
////        )
////    }
//}
