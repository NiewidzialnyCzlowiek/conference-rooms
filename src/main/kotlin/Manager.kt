import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.PagingIterable
import mapper.*
import mu.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

class Manager(session: CqlSession, keyspace: CqlIdentifier) {
    private val mapper = ConferenceRoomsMapper.builder(session).withDefaultKeyspace(keyspace).build()
    private val reservationDao: ReservationDao = mapper.reservationDao()

    fun validateDate(date: LocalDate) {
        val logs = reservationDao.getLogsForDate(date).all()
//            .filter { reservationDao.getCorrection(it.logDate, it.timestamp, it.userId) == null}
        val lastIndex = logs.size - 1
        for (i in 0..lastIndex) {
            val log = logs[i]
            val entries = getEntriesForLog(log).all()
            val isValid = entries.all { it.userId == log.userId }
            if (!isValid) {
                val invalidEntryAuthors = entries.filter { it.userId != log.userId }.groupBy { it.userId }.keys
                val overwritingReservations = logs.takeLast(lastIndex - i)
                    .filter { it.userId in invalidEntryAuthors }
                    .filter { Pair(log.startQuant, log.endQuant) overlaps Pair(it.startQuant, it.endQuant) }
                    .filter { reservationDao.getCorrection(it.logDate, it.timestamp, it.userId) == null}

                logger.debug { "Found conflicting reservations. There are ${overwritingReservations.size} reservations that overlap with reservation $log" }
                overwritingReservations.forEach { logToDelete ->
                    reservationDao.createCorrection(
                        ReservationCorrection.fromLog(logToDelete,
                            operation = "DELETED - This reservation overwritten an earlier reservation. This reservation has been revoked."))

                    reservationDao.deleteReservationByMonth(ReservationByMonth.fromReservationLog(logToDelete))

                    (logToDelete.startQuant!!..logToDelete.endQuant!!).forEach { quant ->
                        reservationDao.deleteEntry(
                            ReservationEntry(roomId = logToDelete.roomId,
                                date = logToDelete.reservationDate,
                                quant = quant,
                                userId = logToDelete.userId)) }
                }
                logger.debug { "Recreating reservation $log" }
                reservationDao.createEntriesForQuantRange(ReservationEntry(log.roomId, log.reservationDate, 0, log.userId), log.startQuant!!, log.endQuant!!)
            }
        }
    }

    fun getCorrectionsForDate(date: LocalDate): List<ReservationCorrection> {
        return reservationDao.getCorrectionsForDate(date).all()
    }

    private fun getEntriesForLog(log: ReservationLog): PagingIterable<ReservationEntry> {
        return reservationDao.getEntriesForTimeRange(log.roomId, log.logDate, log.startQuant, log.endQuant)
    }

    companion object {
        private val MANAGER_ID = "MANAGER"

        infix fun Pair<TimeQuant?, TimeQuant?>.overlaps(range: Pair<TimeQuant?, TimeQuant?>): Boolean {
            return this.first!! <= range.second!! && this.second!! <= range.second!!
        }
    }
}