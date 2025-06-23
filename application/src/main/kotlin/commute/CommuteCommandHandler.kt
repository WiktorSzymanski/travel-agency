package commute

import aggregates.Commute

class CommuteCommandHandler {
    fun handle(command: CommuteCommand) {
        when (command) {
            is CreateCommuteCommand -> handleCreateCommuteCommand(command)
            is CancelCommuteCommand -> handleCancelCommuteCommand(command)
            is CommuteDepartureCommand -> handleCommuteDepartureCommand(command)
            is CommuteArrivalCommand -> handleCommuteArrivalCommand(command)
            is BookSeatCommand -> handleBookSeatCommand(command)
            is CancelBookedSeatCommand -> handleCancelBookedSeatCommand(command)
        }
    }

    fun handleCreateCommuteCommand(command: CreateCommuteCommand) {
        val commute =
            Commute(
                commuteId = command.commuteId,
                departure = command.departure,
                arrival = command.arrival,
                seats = command.seats,
            )

        // SAVE or something
    }

    fun handleCancelCommuteCommand(command: CancelCommuteCommand) {
    }

    fun handleCommuteDepartureCommand(command: CommuteDepartureCommand) {
    }

    fun handleCommuteArrivalCommand(command: CommuteArrivalCommand) {
    }

    fun handleBookSeatCommand(command: BookSeatCommand) {
    }

    fun handleCancelBookedSeatCommand(command: CancelBookedSeatCommand) {
    }
}
