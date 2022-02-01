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
data class ReservationLog(@PartitionKey(0) val date: LocalDate?,
                          @ClusteringColumn(0) val timestamp: Instant?,
                          @ClusteringColumn(1) val userId: UUID?,
                          val roomId: Int?,
                          val operation: String?,
                          val startQuant: TimeQuant?,
                          val endQuant: TimeQuant?) {

    enum class Operation(operation: String) {
        CREATE("CREATE"),
        DELETE("DELETE"),
        CORRECT("CORRECT")
    }
}

@Entity
@CqlName("reservation_correction")
data class ReservationCorrection(@PartitionKey(0) val date: LocalDate?,
                                    @ClusteringColumn(0) val timestamp: Instant?,
                                    @ClusteringColumn(1) val userId: UUID?,
                                    val operation: String?) {

    companion object {
        fun fromLog(log: ReservationLog, operation: String): ReservationCorrection {
            return ReservationCorrection(date = log.date,
                                         timestamp = log.timestamp,
                                         userId = log.userId,
                                         operation = operation)
        }
    }
}

@Dao
interface ReservationDao {
    @Insert
    @StatementAttributes(consistencyLevel = "ONE")
    fun createEntry(reservationEntry: ReservationEntry)

    @StatementAttributes(consistencyLevel = "ONE")
    @QueryProvider(
        providerClass = CreateReservationEntriesForQuantRange::class,
        entityHelpers = [ReservationEntry::class])
    fun createEntriesForQuantRange(entry: ReservationEntry, startQuant: TimeQuant, endQuant: TimeQuant)

    @Select
    fun getEntry(roomId: Int?, date: LocalDate?, quant: TimeQuant?): ReservationEntry?

    @Select
    @StatementAttributes(consistencyLevel = "QUORUM")
    fun getEntriesForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationEntry>

    @StatementAttributes(consistencyLevel = "ONE")
    @QueryProvider(
        providerClass = GetReservationEntriesRangeQueryProvider::class,
        entityHelpers = [ReservationEntry::class])
    fun getEntriesForTimeRange(roomId: Int?, date: LocalDate?, startQuant: TimeQuant?, endQuant: TimeQuant?): PagingIterable<ReservationEntry>

    @Update
    fun updateEntry(template: ReservationEntry)

    @Select
    fun getLogsForDate(date: LocalDate?): PagingIterable<ReservationLog>

    @Insert
    @StatementAttributes(consistencyLevel = "ONE")
    fun createLog(reservationLog: ReservationLog)

    @Select
    @StatementAttributes(consistencyLevel = "QUORUM")
    fun getCorrectionsForDate(date: LocalDate?): PagingIterable<ReservationCorrection>

    @Insert
    @StatementAttributes(consistencyLevel = "QUORUM")
    fun createCorrection(reservationCorrection: ReservationCorrection)
}