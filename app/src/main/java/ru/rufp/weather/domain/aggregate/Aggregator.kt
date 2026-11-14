package ru.rufp.weather.domain.aggregate

import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.AggregatedForecast
import ru.rufp.weather.domain.model.AggregatedHour
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class WeightCalculator {
    data class Result(val weights: Map<String, Double>, val warmUp: Boolean)

    fun compute(
        errors: Map<String, Double>,
        totalObservations: Int,
        minObservations: Int = 20
    ): Result {
        if (errors.isEmpty()) return Result(emptyMap(), true)
        if (totalObservations < minObservations) {
            val equal = 1.0 / errors.size
            return Result(errors.keys.associateWith { equal }, true)
        }
        val raw = errors.mapValues { (_, err) ->
            if (err.isNaN()) 1.0 / 0.02 else 1.0 / (err.coerceAtLeast(0.0) + 0.02)
        }
        val sum = raw.values.sum()
        return Result(raw.mapValues { it.value / sum }, false)
    }
}

class PrecipitationAggregator {

    fun aggregate(
        location: GeoPoint,
        locationName: String,
        issuedAt: Instant,
        weightedForecasts: List<Pair<Double, ru.rufp.weather.domain.model.ProviderForecast>>
    ): AggregatedForecast {
        val valid = weightedForecasts.filter { it.second.hours.isNotEmpty() }
        val start = valid.flatMap { it.second.hours.map { h -> h.time } }.minOrNull() ?: issuedAt
        val end = valid.flatMap { it.second.hours.map { h -> h.time } }.maxOrNull() ?: issuedAt
        val first = start.truncatedTo(ChronoUnit.HOURS)
        val last = end.truncatedTo(ChronoUnit.HOURS)

        val hours = mutableListOf<AggregatedHour>()
        var t = first
        while (!t.isAfter(last)) {
            val hour = aggregateHour(t, valid)
            if (hour != null) hours.add(hour)
            t = t.plus(1, ChronoUnit.HOURS)
        }

        val interval = PrecipIntervalDetector.detect(hours)
        val spreadHours = hours.filter { it.probability > 0.05 }
        val disagree = valid.size >= 2 && spreadHours.any { it.spread > 0.25 }

        return AggregatedForecast(
            location = ru.rufp.weather.domain.model.SavedLocation(
                id = 0,
                name = locationName,
                country = "",
                latitude = location.latitude,
                longitude = location.longitude
            ),
            issuedAt = issuedAt,
            hours = hours,
            interval = interval,
            sourcesDisagree = disagree
        )
    }

    private fun aggregateHour(
        time: Instant,
        weighted: List<Pair<Double, ru.rufp.weather.domain.model.ProviderForecast>>
    ): AggregatedHour? {
        val points = weighted.mapNotNull { (w, f) ->
            f.hours.minByOrNull { kotlin.math.abs(it.time.toEpochMilli() - time.toEpochMilli()) }
                ?.takeIf { kotlin.math.abs(it.time.toEpochMilli() - time.toEpochMilli()) < 90 * 60 * 1000L }
                ?.let { w to it }
        }
        if (points.isEmpty()) return null

        var wsum = 0.0
        var pSum = 0.0
        var iSum = 0.0
        var tSum = 0.0
        var tW = 0.0
        var atSum = 0.0
        var windSum = 0.0
        var humSum = 0.0
        var pressSum = 0.0
        var extraW = 0.0

        for ((w, p) in points) {
            wsum += w
            val prob = p.precipProbability ?: (if ((p.precipIntensity ?: 0.0) > 0.05) 0.7 else 0.05)
            pSum += w * prob
            val intensity = p.precipIntensity ?: 0.0
            iSum += w * intensity
            p.temperature?.let { tSum += w * it; tW += w }
            p.apparentTemperature?.let { atSum += w * it; extraW += w }
            p.windSpeed?.let { windSum += w * it }
            p.humidity?.let { humSum += w * it }
            p.pressure?.let { pressSum += w * it }
        }

        val probs = points.map { (_, p) ->
            p.precipProbability ?: (if ((p.precipIntensity ?: 0.0) > 0.05) 0.7 else 0.05)
        }
        val meanP = pSum / wsum
        val variance = probs.sumOf { it * it } / probs.size - meanP * meanP
        val std = sqrt(max(0.0, variance))

        val avgTemp = if (tW > 0) tSum / tW else null
        val type = voteType(points, avgTemp)

        return AggregatedHour(
            time = time,
            probability = meanP.coerceIn(0.0, 1.0),
            minProbability = min(probs.min(), meanP),
            maxProbability = max(probs.max(), meanP),
            spread = std,
            intensity = iSum / wsum,
            type = type,
            temperature = avgTemp,
            apparentTemperature = if (extraW > 0) atSum / extraW else avgTemp,
            windSpeed = windSum / wsum,
            humidity = humSum / wsum,
            pressure = pressSum / wsum
        )
    }

    private fun voteType(
        points: List<Pair<Double, ru.rufp.weather.domain.model.HourPoint>>,
        avgTemp: Double?
    ): ru.rufp.weather.domain.model.PrecipType {
        val votes = mutableMapOf<ru.rufp.weather.domain.model.PrecipType, Double>()
        for ((w, p) in points) {
            val type = p.precipType ?: ru.rufp.weather.domain.model.PrecipType.NONE
            val boost = if (type == ru.rufp.weather.domain.model.PrecipType.HAIL ||
                type == ru.rufp.weather.domain.model.PrecipType.THUNDERSTORM
            ) 1.5 else 1.0
            votes[type] = (votes[type] ?: 0.0) + w * boost
        }
        val noneScore = votes[ru.rufp.weather.domain.model.PrecipType.NONE] ?: 0.0
        val precipScore = votes.filter { it.key.isPrecipitation }.values.sum()
        if (precipScore < noneScore) return ru.rufp.weather.domain.model.PrecipType.NONE

        val precipVotes = votes.filter { it.key.isPrecipitation }
        val top = precipVotes.maxByOrNull { it.value }?.key ?: ru.rufp.weather.domain.model.PrecipType.RAIN

        val snowLike = setOf(
            ru.rufp.weather.domain.model.PrecipType.SNOW,
            ru.rufp.weather.domain.model.PrecipType.WET_SNOW
        )
        if (avgTemp != null && avgTemp >= -1.5 && avgTemp <= 1.5) {
            val snow = precipVotes.filter { it.key in snowLike }.values.sum()
            val rain = precipVotes.filter {
                it.key in setOf(ru.rufp.weather.domain.model.PrecipType.RAIN, ru.rufp.weather.domain.model.PrecipType.DRIZZLE, ru.rufp.weather.domain.model.PrecipType.HEAVY_RAIN)
            }.values.sum()
            if (snow > 0 && rain > 0 && max(snow, rain) / (snow + rain) < 0.75) {
                return ru.rufp.weather.domain.model.PrecipType.WET_SNOW
            }
        }
        return top
    }
}

object PrecipIntervalDetector {
    const val PROB_THRESHOLD = 0.5
    const val INTENSITY_THRESHOLD = 0.1

    fun detect(hours: List<ru.rufp.weather.domain.model.AggregatedHour>): ru.rufp.weather.domain.model.PrecipInterval? {
        val active = hours.filter {
            it.probability >= PROB_THRESHOLD || it.intensity >= INTENSITY_THRESHOLD
        }
        if (active.isEmpty()) return null

        var best: List<ru.rufp.weather.domain.model.AggregatedHour> = emptyList()
        var current = mutableListOf<ru.rufp.weather.domain.model.AggregatedHour>()
        var prev: Instant? = null
        for (h in active) {
            if (prev != null && ChronoUnit.HOURS.between(prev, h.time) > 1) {
                if (current.size > best.size) best = current.toList()
                current.clear()
            }
            current.add(h)
            prev = h.time
        }
        if (current.size > best.size) best = current.toList()
        if (best.isEmpty()) return null

        val peak = best.maxByOrNull { it.probability + it.intensity } ?: return null
        val peakTime = peak.time.plus(30, ChronoUnit.MINUTES)
        return ru.rufp.weather.domain.model.PrecipInterval(
            start = best.first().time,
            end = best.last().time.plus(1, ChronoUnit.HOURS),
            peakTime = peakTime,
            peakProbability = peak.probability,
            type = peak.type
        )
    }
}
