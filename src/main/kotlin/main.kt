import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import mapper.ConferenceRoomsMapper
import client.Client
import mapper.QuantCalculus.MAX_QUANT
import mu.KotlinLogging
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

object App {
    private val KEYSPACE_ID = CqlIdentifier.fromCql("conference_rooms")

    @JvmStatic fun main(args: Array<String>) {
        val session = CqlSession.builder().withAuthCredentials("cassandra", "cassandra").build()
        maybeCreateSchema(session)
        val mapper: ConferenceRoomsMapper = ConferenceRoomsMapper.builder(session).withDefaultKeyspace(KEYSPACE_ID).build()
        val reservationDao = mapper.reservationDao()

        val minDay = LocalDate.of(2022, 1, 1).toEpochDay()
        val maxDay = LocalDate.of(2022, 2, 1).toEpochDay()
        val random = Random(Instant.now().toEpochMilli())
        val userIds = (0..10).map { UUID.randomUUID() }

        val client = Client(reservationDao)
        for (i in 1..100) {
            val randomRoomId = (1..10).random()
            val randomDay = random.nextLong(minDay, maxDay)
            val day = LocalDate.ofEpochDay(randomDay)
            val userId = userIds.random()
            val randomStartQuant = (0..MAX_QUANT).random()
            val randomEndQuant = (randomStartQuant.. MAX_QUANT).random()

            client.createReservation(randomRoomId, day, randomStartQuant, randomEndQuant, userId)
        }
        val today = LocalDate.now()
        val reservationLogs = reservationDao.getLogsForDate(today).all()
        logger.info { "Reservation logs (${reservationLogs.size}):" }
        for (entry in reservationLogs) { logger.info { entry } }
        logger.info { "Launching manager to find incorrect reservations" }
        val manager = Manager(mapper)
        manager.validateDate(today)

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