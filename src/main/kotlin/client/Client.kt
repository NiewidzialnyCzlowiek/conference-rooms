package client

import mapper.ReservationEntry
import mapper.ReservationLog
import mapper.TimeQuant
import mapper.ReservationDao
import mapper.QuantCalculus.toQuant
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class Client(private val entryDao: ReservationDao) {

    fun createReservation(roomId: Int?, date: LocalDate?, quant: TimeQuant?, endQuant: TimeQuant?) {
        val userId = UUID.randomUUID()

        val reservationEntry = ReservationEntry(
            roomId = roomId,
            date = date,
            quant = quant,
            userId = userId
        )

        /* val quantCounter = endQuant - quant

        list reservationList
        for (i in quantCounter) {
          reservationList.add = reservationEntry.copy(quant += i)
        }
        */

        val reservationLog = ReservationLog(roomId = roomId,
            date = date,
            timestamp = Instant.now(),
            userId = userId,
            operation = ReservationLog.Operation.CREATE.name,
            startQuant = quant,
            endQuant = endQuant)

        val entriesForTimeRange = entryDao.getEntriesForTimeRange(roomId = 1,
            date = LocalDate.now(),
            startQuant = quant,
            endQuant = endQuant
        )

        val reservation = entriesForTimeRange.last()
        if (reservation == null) {
            entryDao.createEntry(reservationEntry)
            entryDao.createLog(reservationLog)
        } else {
            println("The selected time range is occupied, please select a different date\n")
            println("The nearest free room at $reservation,\n ")
            print("do you want to book this date? (Y for yes, N for no): ")
            val clientInput = readLine()

            if (clientInput.equals("Y", ignoreCase = true)) {
                //print("Input reservation end time: ")
                //val correctedEndQuant = readLine()
                //val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm");
                //val dt = LocalDate.parse(correctedEndQuant, formatter);
                val correctedEntry = ReservationEntry(
                    roomId = roomId,
                    date = date,
                    quant = reservation.quant?.plus(1),
                    userId = userId
                )

                val correctedLog = ReservationLog(roomId = roomId,
                    date = date,
                    timestamp = Instant.now(),
                    userId = userId,
                    operation = ReservationLog.Operation.CREATE.name,
                    startQuant = reservation.quant?.plus(1),
                    endQuant = reservation.quant?.plus(2)
                )

                entryDao.createEntry(correctedEntry)
                entryDao.createLog(correctedLog)

            }

        }

    }
}