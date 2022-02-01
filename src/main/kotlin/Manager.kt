import com.datastax.oss.driver.api.core.PagingIterable
import mapper.*
import mu.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

class Manager(objectMapper: ConferenceRoomsMapper) {
    val reservationDao: ReservationDao = objectMapper.reservationDao()

    fun validateDate(date: LocalDate) {
        reservationDao.getLogsForDate(date).forEach {
            val isValid = validateLog(it)
            if (!isValid) {
                repairLog(it)
            }
        }
    }

    private fun validateLog(log: ReservationLog): Boolean {
        val reservationEntries = getEntriesForLog(log)
        return reservationEntries.all { it.userId == log.userId }
    }

    private fun repairLog(logToRepair: ReservationLog) {
        val reservationEntries = getEntriesForLog(logToRepair)
        val invalidEntries = reservationEntries.filter { it.userId != logToRepair.userId }
        for (invalidEntry in invalidEntries) {
            reservationDao.updateEntry(
                invalidEntry.copy(userId = logToRepair.userId))
        }
        val correction = ReservationCorrection.fromLog(logToRepair, "$MANAGER_ID - revert reservation overwrite")
        reservationDao.createCorrection(correction)
        logger.warn { "Created correction: $correction" }
    }

    private fun getEntriesForLog(log: ReservationLog): PagingIterable<ReservationEntry> {
        return reservationDao.getEntriesForTimeRange(log.roomId, log.date, log.startQuant, log.endQuant)
    }

    companion object {
        private val MANAGER_ID = "MANAGER"
    }
}