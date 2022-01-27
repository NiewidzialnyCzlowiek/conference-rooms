package mapper

import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.mapper.annotations.*
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
data class ReservationLog(@PartitionKey(0) val roomId: Int?,
                          @PartitionKey(1) val date: LocalDate?,
                          @ClusteringColumn(0) val timestamp: Instant?,
                          @ClusteringColumn(1) val userId: UUID?,
                          val operation: String?,
                          val startQuant: TimeQuant?,
                          val endQuant: TimeQuant?) {

    enum class Operation(operation: String) {
        CREATE("CREATE"),
        DELETE("DELETE"),
        CORRECT("CORRECT")
    }
}

@Dao
interface ReservationDao {
    @Insert
    @StatementAttributes(consistencyLevel = "ONE")
    fun createEntry(reservationEntry: ReservationEntry)

    @Select
    fun getEntry(roomId: Int?, date: LocalDate?, hour: Int?, quant: TimeQuant?): ReservationEntry?

//    TODO
//    @Select
//    fun getEntryRange(roomId: Int?, date: LocalDate?, fromQuant: TimeQuant, toQuant: TimeQuant)

    @Select
    @StatementAttributes(consistencyLevel = "QUORUM")
    fun getEntriesForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationEntry>

    @Select
    @StatementAttributes(consistencyLevel = "ONE")
    fun getEntriesForTimeRange(roomId: Int?, date: LocalDate?, startQuant: TimeQuant?, endQuant: TimeQuant?): PagingIterable<ReservationEntry>

    @Update
    fun updateEntry(template: ReservationEntry)

    @Select
    fun getLogsForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationLog>

    @Insert
    fun createLog(reservationLog: ReservationLog)
}