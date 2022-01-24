package mapper

import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.mapper.annotations.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@CqlName("reservation_entry")
data class ReservationEntry(@PartitionKey(0) val roomId: Int?,
                            @PartitionKey(1) val date: LocalDate?,
                            @ClusteringColumn(0) val hour: Int?,
                            @ClusteringColumn(1) val quant: Int?,
                            val userId: UUID?)


@Entity
@CqlName("reservation_log")
data class ReservationLog(@PartitionKey(0) val roomId: Int?,
                          @PartitionKey(1) val date: LocalDate?,
                          @ClusteringColumn(0) val timestamp: Instant?,
                          @ClusteringColumn(1) val userId: UUID?,
                          val operation: String?,
                          val startHour: Int?,
                          val startQuant: Int?,
                          val endHour: Int?,
                          val endQuant: Int?) {

    enum class Operation(operation: String) {
        CREATE("CREATE"),
        DELETE("DELETE"),
        CORRECT("CORRECT")
    }
}

@Dao
interface ReservationDao {
    @Insert
    fun createEntry(reservationEntry: ReservationEntry)

    @Select
    fun getEntry(roomId: Int?, date: LocalDate?, hour: Int?, quant: Int?): ReservationEntry?

    @Select
    fun getEntriesForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationEntry>

    @Select
    fun getEntriesForHour(roomId: Int?, date: LocalDate?, hour: Int?): PagingIterable<ReservationEntry>

    @Update
    fun updateEntry(template: ReservationEntry)

    @Select
    fun getLogsForDate(roomId: Int?, date: LocalDate?): PagingIterable<ReservationLog>

    @Insert
    fun createLog(reservationLog: ReservationLog)
}