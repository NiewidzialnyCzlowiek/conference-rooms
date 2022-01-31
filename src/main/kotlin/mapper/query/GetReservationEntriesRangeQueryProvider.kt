package mapper.query

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker
import mapper.ReservationEntry
import mapper.TimeQuant
import java.time.LocalDate

class GetReservationEntriesRangeQueryProvider(
    context: MapperContext,
    private val reservationEntryHelper: EntityHelper<ReservationEntry>
) {
    private val session: CqlSession = context.session
    private val preparedSelectEntries: PreparedStatement

    fun getEntriesForTimeRange(roomId: Int?, date: LocalDate?, startQuant: TimeQuant?, endQuant: TimeQuant?): PagingIterable<ReservationEntry> {
        val boundSelectEntries = preparedSelectEntries.bind(roomId, date, startQuant, endQuant)
        val reservationEntries = session.execute(boundSelectEntries).map {
            reservationEntryHelper[it, false]
        }
        return reservationEntries
    }

    init {
        preparedSelectEntries = session.prepare(
            reservationEntryHelper.selectStart()
                .whereColumn("room_id").isEqualTo(bindMarker())
                .whereColumn("date").isEqualTo(bindMarker())
                .whereColumn("quant").isGreaterThanOrEqualTo(bindMarker())
                .whereColumn("quant").isLessThanOrEqualTo(bindMarker()).asCql())
    }
}