import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs
import com.datastax.oss.protocol.internal.ProtocolConstants
import mapper.ConferenceRoomsMapper
import mapper.ReservationEntry
import mapper.ReservationLog
import mu.KotlinLogging
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
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
                                                hour = 14,
                                                quant = 0,
                                                userId = userId)
        val reservationEntry2 = reservationEntry.copy(quant = 1)
        val reservationLog = ReservationLog(roomId = 1,
                                            date = LocalDate.now(),
                                            timestamp = Instant.now(),
                                            userId = userId,
                                            operation = ReservationLog.Operation.CREATE.name,
                                            startHour = 14,
                                            startQuant = 0,
                                            endHour = 14,
                                            endQuant = 1)

        reservationEntryDao.createEntry(reservationEntry)
        reservationEntryDao.createEntry(reservationEntry2)
        reservationEntryDao.createLog(reservationLog)

        val reservationEntries = reservationEntryDao.getEntriesForDate(1, LocalDate.now())
        val reservationLogs = reservationEntryDao.getLogsForDate(1, LocalDate.now())

        logger.info { "Reservation logs:" }
        for (entry in reservationLogs) { logger.info { entry } }
        logger.info { "Reservation entries:" }
        for (entry in reservationEntries) { logger.info { entry } }
        session.close()
    }

    @Throws(Exception::class)
    private fun maybeCreateSchema(session: CqlSession) {
        session.execute("CREATE KEYSPACE IF NOT EXISTS conference_rooms WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
        session.execute("USE conference_rooms")
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