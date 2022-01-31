import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import mapper.ConferenceRoomsMapper
import mapper.QuantCalculus.toQuant
import mapper.ReservationEntry
import mapper.ReservationLog
import client.Client
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

       /* val client = Client(reservationEntryDao)
        while(true) {
            print("Welcome, do you wish to:\n" +
                    "1. Create a reservation \n" +
                    "2. Delete a reservation \n ")

            val choice = readLine()
            if (choice.equals("1")) {
                print("Choose a room number: ")
                val roomNumber = readLine()?.toInt()
                print("Choose a date: ")
                val date = readLine()

                print("Choose a start hour: ")
                val beginHour = readLine()
                var hour: Int = 0
                if (beginHour != null) {
                    hour = beginHour.toInt()
                }

                print("Choose a start minute(rounded): ")
                val beginMinute = readLine()
                var minute: Int = 0
                if (beginMinute != null) {
                    minute = beginMinute.toInt()
                }
                val startQuant = LocalTime.of(hour, minute).toQuant()

                print("Choose an end hour: ")
                val endHour = readLine()
                var hour2: Int = 0
                if (endHour != null) {
                    hour2 = endHour.toInt()
                }

                print("Choose an end minute(rounded): ")
                val endMinute = readLine()
                var minute2: Int = 0
                if (endMinute != null) {
                    minute2 = endMinute.toInt()
                }
                val endQuant = LocalTime.of(hour2, minute2).toQuant()

                client.createReservation(roomNumber, date, startQuant, endQuant)
            }
        }*/

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