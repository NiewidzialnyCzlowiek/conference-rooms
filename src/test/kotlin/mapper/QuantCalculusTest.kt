package mapper

import mapper.QuantCalculus.localTime
import mapper.QuantCalculus.toQuant
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalTime

internal class QuantCalculusTest {

    @Test
    fun localTimeFullHours() {
        val quant: TimeQuant = 24
        val timeFromQuant = quant.localTime()
        assertEquals(2, timeFromQuant.hour)
        assertEquals(0, timeFromQuant.minute)
    }

    @Test
    fun localTimeHalfHour() {
        val quant: TimeQuant = 42
        val timeFromQuant = quant.localTime()
        assertEquals(3, timeFromQuant.hour)
        assertEquals(30, timeFromQuant.minute)
    }

    @Test
    fun fullHourToQuant() {
        val time = LocalTime.of(14, 0)
        val quant = time.toQuant()
        assertEquals(168, quant)
    }

    @Test
    fun halfHourToQuant() {
        val time = LocalTime.of(14, 30)
        val quant = time.toQuant()
        assertEquals(168 + 6, quant)
    }
}