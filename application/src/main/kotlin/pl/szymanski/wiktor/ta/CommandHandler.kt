package pl.szymanski.wiktor.ta

class CommandHandler {
    fun handle(command: TravelOfferCommand) {
        when (command) {
            is BookTravelOfferCommand -> handle(command)
            is CancelBookTravelOfferCommand -> handle(command)
        }
    }

    fun handle(command: BookTravelOfferCommand) {}

    fun handle(command: CancelBookTravelOfferCommand) {}
}
