package client

import mapper.ReservationEntry
import mapper.ReservationLog
import mapper.TimeQuant
import mapper.ReservationDao
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.util.*

class Client(private val entryDao: ReservationDao) {

    private val logger = KotlinLogging.logger { }

    fun createReservation(roomId: Int?, date: LocalDate?, quant: TimeQuant?, endQuant: TimeQuant?, userId: UUID?) {
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

        val entriesForTimeRange = entryDao.getEntriesForTimeRange(roomId = roomId,
            date = date,
            startQuant = quant,
            endQuant = endQuant)

        val reservation = entriesForTimeRange.one()
        if (reservation == null) {
            entryDao.createEntry(reservationEntry)
            entryDao.createLog(reservationLog)

        } else {
            logger.info("The selected time range is occupied, please select a different date")
            //logger.info("The nearest available room is on $reservation")
        }
    }
}