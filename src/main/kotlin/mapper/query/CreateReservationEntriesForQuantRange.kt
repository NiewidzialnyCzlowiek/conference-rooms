package mapper.query

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.DefaultBatchType
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import mapper.ReservationEntry
import mapper.TimeQuant

class CreateReservationEntriesForQuantRange(
    context: MapperContext,
    private val reservationEntryHelper: EntityHelper<ReservationEntry>
) {
    private val session: CqlSession = context.session
    private val preparedInsertEntry: PreparedStatement

    fun createEntriesForQuantRange(reservationEntry: ReservationEntry, startQuant: TimeQuant, endQuant: TimeQuant) {
        val batch = BatchStatement.builder(DefaultBatchType.LOGGED)
        val insertStatements = (startQuant..endQuant).map { reservationEntry.copy(quant = it) }
                                                     .map { bind(preparedInsertEntry, it, reservationEntryHelper) }
        batch.addStatements(insertStatements).setConsistencyLevel(ConsistencyLevel.ANY)
        session.execute(batch.build())
    }

    init {
        preparedInsertEntry = session.prepare(reservationEntryHelper.insert().asCql())
    }

    companion object {
        private fun <T> bind(preparedStatement: PreparedStatement, entity: T, entityHelper: EntityHelper<T>): BoundStatement {
            val boundStatement = preparedStatement.boundStatementBuilder()
            entityHelper.set(entity, boundStatement, NullSavingStrategy.DO_NOT_SET, false)
            return boundStatement.build()
        }
    }
}
