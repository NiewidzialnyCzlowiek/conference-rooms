package mapper

import java.time.LocalTime

object QuantCalculus {
    private const val quantPeriod = 5
    private const val quantsInOneHour = 60 / quantPeriod

    const val MAX_QUANT = 287

    fun TimeQuant.localTime(): LocalTime {
        val hour = this / quantsInOneHour
        val minute = (this - (hour * quantsInOneHour)) * quantPeriod
        return LocalTime.of(hour, minute)
    }

    fun LocalTime.toQuant(): TimeQuant {
        return this.hour * quantsInOneHour + this.minute / quantPeriod
    }
}
