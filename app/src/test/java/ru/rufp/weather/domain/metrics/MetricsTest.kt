package ru.rufp.weather.domain.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsTest {

    private val sample = listOf(
        WeightedSample(0.9, true, 1.0, 1),
        WeightedSample(0.1, false, 1.0, 1),
        WeightedSample(0.2, true, 1.0, 3),
        WeightedSample(0.7, false, 1.0, 3)
    )

    @Test
    fun brierPerfectForecastIsZero() {
        val samples = listOf(
            WeightedSample(1.0, true, 0.0, 1),
            WeightedSample(0.0, false, 0.0, 1)
        )
        assertEquals(0.0, Metrics.brier(samples), 1e-9)
    }

    @Test
    fun brierWorstForecastIsOne() {
        val samples = listOf(
            WeightedSample(0.0, true, 0.0, 1),
            WeightedSample(1.0, false, 0.0, 1)
        )
        assertEquals(1.0, Metrics.brier(samples), 1e-9)
    }

    @Test
    fun brierConstantFiftyPercent() {
        val samples = (1..100).map {
            WeightedSample(0.5, it % 2 == 0, 0.0, 1)
        }
        assertEquals(0.25, Metrics.brier(samples), 1e-9)
    }

    @Test
    fun brierNaNOnEmpty() {
        assertTrue(Metrics.brier(emptyList()).isNaN())
    }

    @Test
    fun decayReducesOldSamples() {
        val fresh = listOf(WeightedSample(0.8, false, 0.0, 1))
        val old = listOf(WeightedSample(0.8, false, 60.0, 1))
        val freshScore = Metrics.brier(fresh)
        val oldScore = Metrics.brier(old)
        assertTrue(oldScore < freshScore)
    }

    @Test
    fun decayWeightHalfLife() {
        assertEquals(0.5, Metrics.decayWeight(Metrics.HALF_LIFE_DAYS), 1e-9)
        assertEquals(1.0, Metrics.decayWeight(0.0), 1e-9)
    }

    @Test
    fun hitRateAndFalseAlarm() {
        assertEquals(0.5, Metrics.hitRate(sample), 1e-9)
        assertEquals(0.5, Metrics.falseAlarmRate(sample), 1e-9)
    }

    @Test
    fun csiCorrect() {
        val hits = 1
        val fa = 1
        val misses = 1
        assertEquals(hits.toDouble() / (hits + fa + misses), Metrics.csi(sample), 1e-9)
    }

    @Test
    fun maeCorrect() {
        assertEquals(2.0, Metrics.mae(listOf(1.0, 3.0)), 1e-9)
        assertTrue(Metrics.mae(emptyList()).isNaN())
    }

    @Test
    fun typeAccuracyCountsMatches() {
        val samples = listOf(
            WeightedSample(1.0, true, 0.0, 1, forecastType = "RAIN", actualType = "RAIN"),
            WeightedSample(1.0, true, 0.0, 1, forecastType = "SNOW", actualType = "RAIN")
        )
        assertEquals(0.5, Metrics.typeAccuracy(samples), 1e-9)
    }

    @Test
    fun leadBuckets() {
        assertEquals(1, Metrics.leadBucket(1))
        assertEquals(3, Metrics.leadBucket(2))
        assertEquals(3, Metrics.leadBucket(3))
        assertEquals(6, Metrics.leadBucket(5))
        assertEquals(12, Metrics.leadBucket(10))
        assertEquals(24, Metrics.leadBucket(13))
        assertEquals(24, Metrics.leadBucket(40))
    }
}
