package client

import mapper.QuantCalculus.localTime
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

    fun createReservation(roomId: Int, date: LocalDate, quant: TimeQuant, endQuant: TimeQuant, userId: UUID) {
        val entriesForTimeRange = entryDao.getEntriesForTimeRange(roomId = roomId,
            date = date,
            startQuant = quant,
            endQuant = endQuant)

        if (entriesForTimeRange.all().isEmpty()) {
            val reservationEntry = ReservationEntry(
                roomId = roomId,
                date = date,
                quant = quant,
                userId = userId)

            val reservationLog = ReservationLog(roomId = roomId,
                date = LocalDate.now(),
                timestamp = Instant.now(),
                userId = userId,
                operation = ReservationLog.Operation.CREATE.name,
                startQuant = quant,
                endQuant = endQuant)

            entryDao.createEntriesForQuantRange(reservationEntry, quant, endQuant)
            entryDao.createLog(reservationLog)
            logger.info { "Reservation successful $reservationLog" }
        } else {
            logger.warn { "Time range for $date, ${quant.localTime()} - ${endQuant.localTime()} is already occupied" }
        }
    }
}