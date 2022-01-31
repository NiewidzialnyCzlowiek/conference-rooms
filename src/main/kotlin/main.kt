import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs
import com.datastax.oss.protocol.internal.ProtocolConstants
import mapper.ConferenceRoomsMapper
import mapper.QuantCalculus.toQuant
import mapper.ReservationEntry
import mapper.ReservationLog
import mu.KotlinLogging
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

private val logger = KotlinLogging.logger { }

object App {
    private val KEYSPACE_ID = CqlIdentifier.fromCql("conference_rooms")

    @JvmStatic fun main(args: Array<String>) {
        val session = CqlSession.builder().withAuthCredentials("cassandra", "cassandra").build()
        maybeCreateSchema(session)
        val mapper: ConferenceRoomsMapper = ConferenceRoomsMapper.builder(session).withDefaultKeyspace(KEYSPACE_ID).build()

        val userId = UUID.randomUUID()
        val reservationEntryDao = mapper.reservationDao()
        val reservationEntry = ReservationEntry(roomId = 1,
                                                date = LocalDate.now(),
                                                quant = LocalTime.of(12, 10).toQuant(),
                                                userId = userId)
        val reservationEntry2 = reservationEntry.copy(quant = 1)
        val reservationLog = ReservationLog(roomId = 1,
                                            date = LocalDate.now(),
                                            timestamp = Instant.now(),
                                            userId = userId,
                                            operation = ReservationLog.Operation.CREATE.name,
                                            startQuant = LocalTime.of(12,0).toQuant(),
                                            endQuant = LocalTime.of(13,0).toQuant())

        reservationEntryDao.createEntry(reservationEntry)
        reservationEntryDao.createEntry(reservationEntry2)
        reservationEntryDao.createLog(reservationLog)

        val reservationEntries = reservationEntryDao.getEntriesForDate(1, LocalDate.now())
        val reservationLogs = reservationEntryDao.getLogsForDate(LocalDate.now())
        val entriesForTimeRange = reservationEntryDao.getEntriesForTimeRange(roomId = 1,
                                                                             date = LocalDate.now(),
                                                                             startQuant = LocalTime.of(0,0).toQuant(),
                                                                             endQuant = LocalTime.of(14,0).toQuant())

        logger.info { "Reservation logs:" }
        for (entry in reservationLogs) { logger.info { entry } }
        logger.info { "Reservation entries:" }
        for (entry in reservationEntries) { logger.info { entry } }
        logger.info { "Reservation entries for time range:" }
        for (entry in entriesForTimeRange) { logger.info { entry } }
        session.close()
    }

    @Throws(Exception::class)
    private fun maybeCreateSchema(session: CqlSession) {
        for (statement in getStatements("schema.cql")) {
            session.execute(statement)
        }
    }

    @Throws(Exception::class)
    private fun getStatements(fileName: String): List<String> {
        val uri = this.javaClass.classLoader.getResource(fileName)!!.toURI()
        val path = if (uri.toString().contains("!")) {
            // This happens when the file is in a packaged JAR
            val (fs, file) = uri.toString().split("!")
            FileSystems.newFileSystem(URI.create(fs), emptyMap<String, Any>()).getPath(file)
        } else {
            Paths.get(uri)
        }
        return Files.readString(path).split(";")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
    }
}