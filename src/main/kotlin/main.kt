import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import client.Client
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import mapper.QuantCalculus.MAX_QUANT
import mu.KotlinLogging
import java.lang.Integer.min
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

class RandomDataGenerator(private val props: Props, private val client: Client): Runnable {
    override fun run() {
        for (i in 1..props.iterations) {
            val randomRoomId = (1..10).random()
            val randomDay = Random.nextLong(props.minEpochDay, props.maxEpochDay)
            val day = LocalDate.ofEpochDay(randomDay)
            val randomStartQuant = (0..MAX_QUANT).random()
            val randomEndQuant = (randomStartQuant..min(randomStartQuant + 36, MAX_QUANT)).random()

            val created = client.createReservation(randomRoomId, day, randomStartQuant, randomEndQuant)
            if (!created) { // retry
                Thread.sleep(5)
                client.createReservation(randomRoomId, day, randomStartQuant, randomEndQuant)
            }
        }
        client.disconnect()
    }

    data class Props(val roomIds: List<Int>,
                     val iterations: Int,
                     val minEpochDay: Long,
                     val maxEpochDay: Long)
}

object App {
    private val KEYSPACE_ID = CqlIdentifier.fromCql("conference_rooms")

    @JvmStatic fun main(args: Array<String>) {
        val session = CqlSession.builder().withAuthCredentials("cassandra", "cassandra").build()
        try {
            maybeCreateSchema(session)
        } catch (e: Exception) {
            logger.error { e.message }
            session.close()
            return
        }

        val USERS_NO = 100
        val ROOMS_NO = 2
        val RESERVATIONS_PER_USER = 100
        val DATE_FROM = LocalDate.of(2022, 1, 6)
        val DATE_TO = LocalDate.of(2022, 1, 10)

        val clients = (1..USERS_NO).map {
            val userSession = CqlSession.builder().withAuthCredentials("cassandra", "cassandra").build()
            Client(userSession, KEYSPACE_ID, UUID.randomUUID())
        }
        val roomIds = (1..ROOMS_NO).toList()
        val minDay = DATE_FROM.toEpochDay()
        val maxDay = DATE_TO.toEpochDay()
        val dataGeneratorProps = RandomDataGenerator.Props(roomIds, RESERVATIONS_PER_USER, minDay, maxDay)

        val clientThreads = clients.map { Thread(RandomDataGenerator(dataGeneratorProps, it)) }
        clientThreads.forEach { it.run() }
        clientThreads.forEach { it.join() }

        val today = LocalDate.now()
//        val reservationLogs = reservationDao.getLogsForDate(today).all()
//        logger.info { "Reservation logs (${reservationLogs.size}):" }
//        for (entry in reservationLogs) { logger.info { entry } }
        logger.info { "Launching manager to find incorrect reservations" }
        val manager = Manager(session, KEYSPACE_ID)
        manager.validateDate(today)
        val corrections = manager.getCorrectionsForDate(today)
        logger.info { "There are ${corrections.size} corrections for date $today" }
        corrections.forEach { logger.info { it } }
        session.close()
    }

    @Throws(Exception::class)
    private fun maybeCreateSchema(session: CqlSession) {
        for (statement in getStatements("schema.cql")) {
            session.execute(SimpleStatement.newInstance(statement).setExecutionProfileName("slow"))
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