package ru.rufp.weather.domain.aggregate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.rufp.weather.domain.model.AggregatedHour
import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.HourPoint
import ru.rufp.weather.domain.model.PrecipType
import ru.rufp.weather.domain.model.ProviderForecast
import java.time.Instant
import java.time.temporal.ChronoUnit

class AggregatorTest {

    private val base = Instant.parse("2026-11-14T12:00:00Z")

    private fun forecast(
        id: String,
        probs: List<Double>,
        temps: List<Double>? = null,
        types: List<PrecipType>? = null
    ): ProviderForecast = ProviderForecast(
        sourceId = id,
        issuedAt = base,
        hours = probs.mapIndexed { i, p ->
            HourPoint(
                time = base.plus(i.toLong(), ChronoUnit.HOURS),
                precipProbability = p,
                precipIntensity = if (p > 0.4) 0.5 else 0.0,
                precipType = types?.get(i) ?: if (p > 0.4) PrecipType.RAIN else PrecipType.NONE,
                temperature = temps?.get(i),
                apparentTemperature = temps?.get(i),
                windSpeed = 3.0,
                humidity = 70.0,
                pressure = 745.0
            )
        }
    )

    @Test
    fun weightCalculatorColdStartEqualWeights() {
        val result = WeightCalculator().compute(
            errors = mapOf("a" to 0.1, "b" to 0.2),
            totalObservations = 5
        )
        assertTrue(result.warmUp)
        assertEquals(0.5, result.weights["a"]!!, 1e-9)
        assertEquals(0.5, result.weights["b"]!!, 1e-9)
    }

    @Test
    fun weightCalculatorBetterSourceGetsMoreWeight() {
        val result = WeightCalculator().compute(
            errors = mapOf("good" to 0.10, "bad" to 0.30),
            totalObservations = 100
        )
        assertFalse(result.warmUp)
        assertTrue(result.weights["good"]!! > result.weights["bad"]!!)
        assertEquals(1.0, result.weights.values.sum(), 1e-9)
    }

    @Test
    fun aggregateWeightedMeanProbability() {
        val forecasts = listOf(
            0.75 to forecast("a", listOf(0.8, 0.2)),
            0.25 to forecast("b", listOf(0.4, 0.4))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertEquals(0.7, agg.hours[0].probability, 1e-9)
        assertEquals(0.25, agg.hours[1].probability, 1e-9)
    }

    @Test
    fun aggregateMinAndMaxRange() {
        val forecasts = listOf(
            0.5 to forecast("a", listOf(0.9)),
            0.5 to forecast("b", listOf(0.2))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertEquals(0.2, agg.hours[0].minProbability, 1e-9)
        assertEquals(0.9, agg.hours[0].maxProbability, 1e-9)
        assertTrue(agg.sourcesDisagree)
    }

    @Test
    fun noDisagreementWhenClose() {
        val forecasts = listOf(
            0.5 to forecast("a", listOf(0.3)),
            0.5 to forecast("b", listOf(0.4))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertFalse(agg.sourcesDisagree)
    }

    @Test
    fun temperatureAveragedByWeight() {
        val forecasts = listOf(
            0.8 to forecast("a", listOf(0.0), temps = listOf(10.0)),
            0.2 to forecast("b", listOf(0.0), temps = listOf(20.0))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertEquals(12.0, agg.hours[0].temperature!!, 1e-9)
    }

    @Test
    fun nearZeroTemperaturePrefersWetSnow() {
        val forecasts = listOf(
            0.5 to forecast("a", listOf(0.9), temps = listOf(0.5), types = listOf(PrecipType.SNOW)),
            0.5 to forecast("b", listOf(0.9), temps = listOf(0.5), types = listOf(PrecipType.RAIN))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertEquals(PrecipType.WET_SNOW, agg.hours[0].type)
    }

    @Test
    fun thunderstormHasPriority() {
        val forecasts = listOf(
            0.5 to forecast("a", listOf(0.8), types = listOf(PrecipType.THUNDERSTORM)),
            0.5 to forecast("b", listOf(0.8), types = listOf(PrecipType.RAIN))
        )
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertEquals(PrecipType.THUNDERSTORM, agg.hours[0].type)
    }

    @Test
    fun noPrecipitationWhenSourcesSayDry() {
        val forecasts = listOf(1.0 to forecast("a", List(24) { 0.0 }))
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, forecasts
        )
        assertNull(agg.interval)
        assertEquals(24, agg.hours.size)
        assertEquals(PrecipType.NONE, agg.hours[0].type)
    }

    @Test
    fun emptyForecastsProduceEmptyHours() {
        val agg = PrecipitationAggregator().aggregate(
            GeoPoint(55.75, 37.62), "Москва", base, emptyList()
        )
        assertTrue(agg.hours.isEmpty())
    }
}

class IntervalDetectorTest {

    private fun hour(idx: Int, prob: Double, intensity: Double = if (prob > 0.5) 0.6 else 0.0) =
        AggregatedHour(
            time = Instant.parse("2026-11-14T12:00:00Z").plus(idx.toLong(), ChronoUnit.HOURS),
            probability = prob,
            minProbability = prob,
            maxProbability = prob,
            spread = 0.0,
            intensity = intensity,
            type = if (prob > 0.5) PrecipType.RAIN else PrecipType.NONE,
            temperature = 5.0,
            apparentTemperature = 4.0,
            windSpeed = 2.0,
            humidity = 80.0,
            pressure = 740.0
        )

    @Test
    fun detectsSingleInterval() {
        val hours = (0..23).map { hour(it, if (it in 3..5) 0.8 else 0.1) }
        val interval = PrecipIntervalDetector.detect(hours)
        assertNotNull(interval)
        assertEquals(Instant.parse("2026-11-14T15:00:00Z"), interval!!.start)
        assertEquals(Instant.parse("2026-11-14T18:00:00Z"), interval.end)
        assertEquals(0.8, interval.peakProbability, 1e-9)
    }

    @Test
    fun choosesLongestInterval() {
        val hours = (0..23).map {
            hour(it, when (it) {
                in 1..2 -> 0.7
                in 6..9 -> 0.6
                else -> 0.1
            })
        }
        val interval = PrecipIntervalDetector.detect(hours)
        assertNotNull(interval)
        assertEquals(4, java.time.temporal.ChronoUnit.HOURS.between(interval!!.start, interval.end))
    }

    @Test
    fun peakTimeIsHalfHourOffset() {
        val hours = (0..23).map { hour(it, if (it == 4) 0.9 else 0.1) }
        val interval = PrecipIntervalDetector.detect(hours)
        assertEquals(Instant.parse("2026-11-14T16:30:00Z"), interval!!.peakTime)
    }

    @Test
    fun noIntervalWhenAllDry() {
        val hours = (0..23).map { hour(it, 0.2, 0.0) }
        assertNull(PrecipIntervalDetector.detect(hours))
    }

    @Test
    fun gapSplitsIntervals() {
        val hours = (0..23).map {
            hour(it, when (it) {
                in 2..3 -> 0.8
                in 5..6 -> 0.7
                else -> 0.1
            })
        }
        val interval = PrecipIntervalDetector.detect(hours)
        assertNotNull(interval)
        assertEquals(2, java.time.temporal.ChronoUnit.HOURS.between(interval!!.start, interval.end))
    }
}
