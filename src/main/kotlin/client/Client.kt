package client

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import mapper.*
import mapper.QuantCalculus.localTime
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.util.*

private val logger = KotlinLogging.logger { }

class Client(private val session: CqlSession, keyspace: CqlIdentifier, private val userId: UUID) {
    private val mapper = ConferenceRoomsMapper.builder(session).withDefaultKeyspace(keyspace).build()
    private val reservationDao: ReservationDao = mapper.reservationDao()

    fun createReservation(roomId: Int, date: LocalDate, quant: TimeQuant, endQuant: TimeQuant): Boolean {
        val entriesForTimeRange = reservationDao.getEntriesForTimeRange(roomId = roomId,
            date = date,
            startQuant = quant,
            endQuant = endQuant)

        if (entriesForTimeRange.all().isEmpty()) {
            logger.debug { "User $userId making reservation for $roomId on $date from ${quant.localTime()} to ${endQuant.localTime()}" }
            val reservationEntryTemplate = ReservationEntry(
                roomId = roomId,
                date = date,
                quant = quant,
                userId = userId)
            reservationDao.createEntriesForQuantRange(reservationEntryTemplate, quant, endQuant)

            val ownEntries = reservationDao.getEntriesForTimeRange(roomId, date, quant, endQuant).all()
            val reservationOverwritten = ownEntries.any { it.userId != userId }

            if (reservationOverwritten) {
                // remove own entries
                logger.debug { "Reservation could not be completed! Removing existing entries for user $userId, room $roomId, date $date, from ${quant.localTime()} to ${endQuant.localTime()}" }
                ownEntries.forEach { reservationDao.deleteEntry(it) }
            } else {
                // confirm reservation
                val reservationLog = ReservationLog(roomId = roomId,
                    logDate = LocalDate.now(),
                    timestamp = Instant.now(),
                    userId = userId,
                    reservationDate = date,
                    startQuant = quant,
                    endQuant = endQuant)
                logger.debug { "Saving reservation log and reservation info" }
                reservationDao.createLog(reservationLog)
                reservationDao.createReservationByMonth(ReservationByMonth.fromReservationLog(reservationLog))
                logger.info { "Reservation successful $reservationLog" }
                return true
            }
        } else {
            logger.warn { "Time range for $date, ${quant.localTime()} - ${endQuant.localTime()} is already occupied" }
        }
        return false
    }

    fun disconnect() {
        session.close()
    }
}