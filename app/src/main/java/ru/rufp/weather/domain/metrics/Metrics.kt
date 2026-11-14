package ru.rufp.weather.domain.metrics

import kotlin.math.exp
import kotlin.math.ln

data class WeightedSample(
    val probability: Double,
    val actual: Boolean,
    val ageDays: Double,
    val leadHours: Int,
    val temperatureError: Double? = null,
    val forecastType: String? = null,
    val actualType: String? = null
)

object Metrics {
    const val HALF_LIFE_DAYS = 15.0

    fun decayWeight(ageDays: Double, halfLifeDays: Double = HALF_LIFE_DAYS): Double =
        exp(-ageDays * ln(2.0) / halfLifeDays)

    fun brier(samples: List<WeightedSample>): Double {
        val used = samples.filter { it.probability in 0.0..1.0 }
        if (used.isEmpty()) return Double.NaN
        var sum = 0.0
        var wsum = 0.0
        for (s in used) {
            val w = decayWeight(s.ageDays)
            val y = if (s.actual) 1.0 else 0.0
            val err = s.probability - y
            sum += w * err * err
            wsum += w
        }
        return sum / wsum
    }

    fun mae(errors: List<Double>): Double =
        if (errors.isEmpty()) Double.NaN else errors.average()

    fun hitRate(samples: List<WeightedSample>): Double {
        val hits = samples.count { it.actual && it.probability >= 0.5 }
        val observed = samples.count { it.actual }
        return if (observed == 0) Double.NaN else hits.toDouble() / observed
    }

    fun falseAlarmRate(samples: List<WeightedSample>): Double {
        val falseAlarms = samples.count { !it.actual && it.probability >= 0.5 }
        val predicted = samples.count { it.probability >= 0.5 }
        return if (predicted == 0) Double.NaN else falseAlarms.toDouble() / predicted
    }

    fun csi(samples: List<WeightedSample>): Double {
        val hits = samples.count { it.actual && it.probability >= 0.5 }
        val falseAlarms = samples.count { !it.actual && it.probability >= 0.5 }
        val misses = samples.count { it.actual && it.probability < 0.5 }
        val denom = hits + falseAlarms + misses
        return if (denom == 0) Double.NaN else hits.toDouble() / denom
    }

    fun typeAccuracy(samples: List<WeightedSample>): Double {
        val typed = samples.filter { it.forecastType != null && it.actualType != null }
        if (typed.isEmpty()) return Double.NaN
        return typed.count { it.forecastType == it.actualType }.toDouble() / typed.size
    }

    fun leadBucket(leadHours: Int): Int = when {
        leadHours <= 1 -> 1
        leadHours <= 3 -> 3
        leadHours <= 6 -> 6
        leadHours <= 12 -> 12
        else -> 24
    }
}
