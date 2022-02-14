package mapper

import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.mapper.annotations.*
import mapper.query.CreateReservationEntriesForQuantRange
import mapper.query.GetReservationEntriesRangeQueryProvider
import java.time.Instant
import java.time.LocalDate
import java.util.*

typealias TimeQuant = Int

@Entity
@CqlName("reservation_entry")
data class ReservationEntry(@PartitionKey(0) val roomId: Int?,
                            @PartitionKey(1) val date: LocalDate?,
                            @ClusteringColumn(0) val quant: TimeQuant?,
                            val userId: UUID?)


@Entity
@CqlName("reservation_log")
data class ReservationLog(@PartitionKey(0) val logDate: LocalDate?,
                          @ClusteringColumn(0) val timestamp: Instant?,
                          @ClusteringColumn(1) val userId: UUID?,
                          val roomId: Int?,
                          val reservationDate: LocalDate?,
                          val startQuant: TimeQuant?,
                          val endQuant: TimeQuant?)

@Entity
@CqlName("reservation_correction")
data class ReservationCorrection(@PartitionKey(0) val date: LocalDate?,
                                    @ClusteringColumn(0) val timestamp: Instant?,
                                    @ClusteringColumn(1) val userId: UUID?,
                                    val operation: String?) {

    companion object {
        fun fromLog(log: ReservationLog, operation: String): ReservationCorrection {
            return ReservationCorrection(date = log.logDate,
                                         timestamp = log.timestamp,
                                         userId = log.userId,
                                         operation = operation)
        }
    }
}

@Entity
@CqlName("reservation_by_month")
data class ReservationByMonth(@PartitionKey(0) val year: Int?,
                              @PartitionKey(1) val month: Int?,
                              @ClusteringColumn(0) val userId: UUID?,
                              @ClusteringColumn(1) val date: LocalDate?,
                              @ClusteringColumn(2) val startQuant: TimeQuant?,
                              @ClusteringColumn(3) val roomId: Int?,
                              val endQuant: TimeQuant?) {
    companion object {
        fun fromReservationLog(reservationLog: ReservationLog): ReservationByMonth {
            return ReservationByMonth(year = reservationLog.reservationDate?.year,
                                      month = reservationLog.reservationDate?.monthValue,
                                      userId = reservationLog.userId,
                                      date = reservationLog.reservationDate,
                                      startQuant = reservationLog.startQuant,
                                      roomId = reservationLog.roomId,
                                      endQuant = reservationLog.endQuant)
        }
    }
}

@Dao
interface ReservationDao {
    @Insert
    @StatementAttributes(consistencyLevel = "ANY")
    fun createEntry(reservationEntry: ReservationEntry)

    @StatementAttributes(consistencyLevel = "ANY")
    @QueryProvider(
        providerClass = CreateReservationEntriesForQuantRange::class,
        entityHelpers = [ReservationEntry::class])
    fun createEntriesForQuantRange(entry: ReservationEntry, startQuant: TimeQuant, endQuant: TimeQuant)

    @Select
    fun getEntry(roomId: Int?, date: LocalDate?, quant: TimeQuant?): ReservationEntry?

    @Select
    fun getEntriesForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationEntry>

    @StatementAttributes(consistencyLevel = "ONE")
    @QueryProvider(
        providerClass = GetReservationEntriesRangeQueryProvider::class,
        entityHelpers = [ReservationEntry::class])
    fun getEntriesForTimeRange(roomId: Int?, date: LocalDate?, startQuant: TimeQuant?, endQuant: TimeQuant?): PagingIterable<ReservationEntry>

    @Update
    fun updateEntry(template: ReservationEntry)

    @Delete
    fun deleteEntry(entry: ReservationEntry)

    @Select
    fun getLogsForDate(date: LocalDate?): PagingIterable<ReservationLog>

    @Insert
    @StatementAttributes(consistencyLevel = "ONE")
    fun createLog(reservationLog: ReservationLog)

    @Select
    fun getCorrectionsForDate(date: LocalDate?): PagingIterable<ReservationCorrection>

    @Select
    fun getCorrection(date: LocalDate?, timestamp: Instant?, userId: UUID?): ReservationCorrection?

    @Insert
    fun createCorrection(reservationCorrection: ReservationCorrection)

    @Insert
    fun createReservationByMonth(reservationByMonth: ReservationByMonth)

    @Select
    fun getReservationsByMonth(year: Int?, month: Int?): PagingIterable<ReservationByMonth>

    @Update
    fun updateReservationByMonth(reservationByMonth: ReservationByMonth)

    @Delete
    fun deleteReservationByMonth(reservationByMonth: ReservationByMonth)
}