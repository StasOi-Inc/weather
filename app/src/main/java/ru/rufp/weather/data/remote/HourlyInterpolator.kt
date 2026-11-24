package ru.rufp.weather.data.remote

import ru.rufp.weather.domain.model.HourPoint
import java.time.Instant
import java.time.temporal.ChronoUnit

object HourlyInterpolator {

    fun interpolateToHourly(points: List<HourPoint>): List<HourPoint> {
        if (points.size < 2) return points
        val sorted = points.sortedBy { it.time }
        val result = mutableListOf<HourPoint>()
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val aHour = a.time.truncatedTo(ChronoUnit.HOURS)
            val bHour = b.time.truncatedTo(ChronoUnit.HOURS)
            val spanMinutes = ChronoUnit.MINUTES.between(aHour, bHour)
            if (spanMinutes <= 0) {
                result.add(a)
                continue
            }
            var t = aHour
            while (t.isBefore(bHour)) {
                val f = ChronoUnit.MINUTES.between(aHour, t).toDouble() / spanMinutes
                result.add(lerp(a, b, t, f))
                t = t.plus(1, ChronoUnit.HOURS)
            }
        }
        result.add(sorted.last())
        return result
    }

    private fun lerp(a: HourPoint, b: HourPoint, time: Instant, f: Double): HourPoint = HourPoint(
        time = time,
        precipProbability = lerpNullable(a.precipProbability, b.precipProbability, f),
        precipIntensity = lerpNullable(a.precipIntensity, b.precipIntensity, f),
        precipType = if (f < 0.5) a.precipType ?: b.precipType else b.precipType ?: a.precipType,
        temperature = lerpNullable(a.temperature, b.temperature, f),
        apparentTemperature = lerpNullable(a.apparentTemperature, b.apparentTemperature, f)
            ?: lerpNullable(a.temperature, b.temperature, f),
        windSpeed = lerpNullable(a.windSpeed, b.windSpeed, f),
        humidity = lerpNullable(a.humidity, b.humidity, f),
        pressure = lerpNullable(a.pressure, b.pressure, f)
    )

    private fun lerpNullable(a: Double?, b: Double?, f: Double): Double? {
        if (a == null || b == null) return a ?: b
        return a + (b - a) * f
    }
}
