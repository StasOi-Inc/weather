package ru.rufp.weather.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.rufp.weather.domain.model.HourPoint
import ru.rufp.weather.domain.model.PrecipType
import java.time.Instant
import java.time.temporal.ChronoUnit

class HourlyInterpolatorTest {

    private val base = Instant.parse("2026-11-14T12:00:00Z").truncatedTo(ChronoUnit.HOURS)

    private fun point(hourOffset: Long, prob: Double, temp: Double, type: PrecipType = PrecipType.RAIN) =
        HourPoint(
            time = base.plus(hourOffset, ChronoUnit.HOURS),
            precipProbability = prob,
            precipIntensity = prob,
            precipType = type,
            temperature = temp,
            apparentTemperature = temp - 1.0,
            windSpeed = 3.0,
            humidity = 70.0,
            pressure = 745.0
        )

    @Test
    fun interpolatesLinearlyBetweenThreeHourPoints() {
        val result = HourlyInterpolator.interpolateToHourly(
            listOf(point(0, 0.0, 10.0), point(3, 0.6, 13.0))
        )
        assertEquals(4, result.size)
        assertEquals(base, result[0].time)
        assertEquals(base.plus(1, ChronoUnit.HOURS), result[1].time)
        assertEquals(base.plus(3, ChronoUnit.HOURS), result[3].time)
        assertEquals(0.2, result[1].precipProbability!!, 1e-9)
        assertEquals(11.0, result[1].temperature!!, 1e-9)
        assertEquals(0.4, result[2].precipProbability!!, 1e-9)
    }

    @Test
    fun singlePointNotInterpolated() {
        val result = HourlyInterpolator.interpolateToHourly(listOf(point(0, 0.3, 5.0)))
        assertEquals(1, result.size)
        assertEquals(0.3, result[0].precipProbability!!, 1e-9)
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertTrue(HourlyInterpolator.interpolateToHourly(emptyList()).isEmpty())
    }

    @Test
    fun typeTakenFromNearestPoint() {
        val result = HourlyInterpolator.interpolateToHourly(
            listOf(point(0, 0.0, 10.0, PrecipType.NONE), point(3, 0.6, 13.0, PrecipType.SNOW))
        )
        assertEquals(PrecipType.NONE, result[1].precipType)
    }

    @Test
    fun nullFieldsFallBackToNonNullSide() {
        val a = point(0, 0.1, 10.0).copy(windSpeed = null)
        val result = HourlyInterpolator.interpolateToHourly(listOf(a, point(2, 0.2, 12.0)))
        assertEquals(3.0, result[1].windSpeed!!, 1e-9)
    }

    @Test
    fun outsideRangeNotGenerated() {
        val result = HourlyInterpolator.interpolateToHourly(
            listOf(point(0, 0.0, 10.0), point(3, 0.3, 11.0))
        )
        assertNull(result.firstOrNull { it.time.isAfter(base.plus(3, ChronoUnit.HOURS)) })
        assertEquals(4, result.size)
    }
}
