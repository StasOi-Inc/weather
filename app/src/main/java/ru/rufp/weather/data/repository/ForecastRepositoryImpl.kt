package ru.rufp.weather.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.rufp.weather.BuildConfig
import ru.rufp.weather.data.local.ForecastSnapshotEntity
import ru.rufp.weather.data.local.ObservationEntity
import ru.rufp.weather.data.local.ObservationsDao
import ru.rufp.weather.data.local.SnapshotsDao
import ru.rufp.weather.data.local.SourceMetricsDao
import ru.rufp.weather.data.local.SourceMetricEntity
import ru.rufp.weather.data.local.SourceStateDao
import ru.rufp.weather.data.local.SourceStateEntity
import ru.rufp.weather.data.remote.OpenMeteoApi
import ru.rufp.weather.data.remote.PrecipMappers
import ru.rufp.weather.data.remote.TimeParsers
import ru.rufp.weather.domain.aggregate.PrecipitationAggregator
import ru.rufp.weather.domain.aggregate.WeightCalculator
import ru.rufp.weather.domain.metrics.Metrics
import ru.rufp.weather.domain.metrics.WeightedSample
import ru.rufp.weather.domain.model.AggregatedForecast
import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.PrecipType
import ru.rufp.weather.domain.model.SavedLocation
import ru.rufp.weather.domain.model.SourceStatus
import ru.rufp.weather.domain.provider.ProviderRegistry
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RawHour(
    val time: Long,
    val prob: Double?,
    val mm: Double?,
    val type: String?,
    val temp: Double?
)

interface ForecastRepository {
    fun cachedForecast(): Flow<CachedForecast?>
    suspend fun cachedForecastOnce(): CachedForecast?
    suspend fun sync(): Boolean
    fun sourceRows(): Flow<List<SourceStateEntity>>
    suspend fun buildSourceRows(): List<SourceRow>
    suspend fun toggleSource(sourceId: String, enabled: Boolean)
    suspend fun recomputeMetrics()
    suspend fun rainImminentNextTwoHours(): Boolean
}

@Singleton
class ForecastRepositoryImpl @Inject constructor(
    private val registry: ProviderRegistry,
    private val locationRepository: LocationRepositoryImpl,
    private val snapshotsDao: SnapshotsDao,
    private val observationsDao: ObservationsDao,
    private val sourceStateDao: SourceStateDao,
    private val sourceMetricsDao: SourceMetricsDao,
    private val openMeteoApi: OpenMeteoApi,
    private val cache: ForecastCache,
    private val json: Json
) : ForecastRepository {

    private val aggregator = PrecipitationAggregator()
    private val weightCalculator = WeightCalculator()

    override fun cachedForecast(): Flow<CachedForecast?> = cache.flow()

    override suspend fun cachedForecastOnce(): CachedForecast? = cache.cached()

    override fun sourceRows(): Flow<List<SourceStateEntity>> = sourceStateDao.all()

    override suspend fun toggleSource(sourceId: String, enabled: Boolean) {
        val current = sourceStateDao.allOnce().firstOrNull { it.sourceId == sourceId }
        sourceStateDao.upsert(
            SourceStateEntity(
                sourceId = sourceId,
                enabled = enabled,
                lastStatus = current?.lastStatus ?: SourceStatus.OK.name,
                lastSuccessAt = current?.lastSuccessAt
            )
        )
    }

    override suspend fun rainImminentNextTwoHours(): Boolean {
        val cached = cache.cached() ?: return false
        val limit = Instant.now().plus(2, ChronoUnit.HOURS).toEpochMilli()
        return cached.hours.any {
            it.timeEpochMillis in Instant.now().toEpochMilli()..limit && it.probability >= 0.6
        }
    }

    override suspend fun sync(): Boolean {
        val location = locationRepository.currentLocationOnce()
            ?: return false
        val now = Instant.now()

        sourceStateDao.insertIfMissing(
            registry.all.map {
                SourceStateEntity(it.id, enabled = true, lastStatus = SourceStatus.OK.name, lastSuccessAt = null)
            }
        )
        val states = sourceStateDao.allOnce().associateBy { it.sourceId }

        val fetched: List<Pair<String, ru.rufp.weather.domain.model.ProviderForecast>> = coroutineScope {
            registry.all.map { provider ->
                async {
                    val state = states[provider.id]
                    val enabled = state?.enabled ?: true
                    val key = if (provider.requiresKey) apiKeyFor(provider.id) else null
                    if (!enabled || (provider.requiresKey && key.isNullOrBlank())) return@async null
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(9_000) {
                            provider.fetch(GeoPoint(location.latitude, location.longitude), key)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().map { it.sourceId to it }
        }

        val successful = fetched.map { it.first }.toSet()
        for (provider in registry.all) {
            val prev = states[provider.id]
            if ((prev?.enabled ?: true) && !(provider.requiresKey && apiKeyFor(provider.id).isNullOrBlank())) {
                sourceStateDao.upsert(
                    SourceStateEntity(
                        sourceId = provider.id,
                        enabled = prev?.enabled ?: true,
                        lastStatus = if (provider.id in successful) SourceStatus.OK.name else SourceStatus.ERROR.name,
                        lastSuccessAt = if (provider.id in successful) now.toEpochMilli() else prev?.lastSuccessAt
                    )
                )
            }
        }

        saveSnapshots(location.id, fetched, now)
        runCatching {
            kotlinx.coroutines.withTimeoutOrNull(9_000) {
                fetchObservations(location.id, location.latitude, location.longitude)
            }
        }
        snapshotsDao.pruneOlderThan(location.id, now.minus(35, ChronoUnit.DAYS).toEpochMilli())

        if (successful.isEmpty()) return false

        runCatching { recomputeMetrics() }
        val aggregated = aggregate(location, now)
        cache.save(aggregated.toCached())
        return true
    }

    private suspend fun saveSnapshots(
        locationId: Long,
        fetched: List<Pair<String, ru.rufp.weather.domain.model.ProviderForecast>>,
        now: Instant
    ) {
        val entities = fetched.flatMap { (sourceId, forecast) ->
            val raw = json.encodeToString(
                forecast.hours.map {
                    RawHour(it.time.toEpochMilli(), it.precipProbability, it.precipIntensity, it.precipType?.name, it.temperature)
                }
            )
            forecast.hours.mapIndexed { index, h ->
                ForecastSnapshotEntity(
                    sourceId = sourceId,
                    locationId = locationId,
                    issuedAt = forecast.issuedAt.toEpochMilli(),
                    targetHour = h.time.toEpochMilli(),
                    precipProbability = h.precipProbability,
                    precipType = h.precipType?.name,
                    precipIntensity = h.precipIntensity,
                    temperature = h.temperature,
                    apparentTemperature = h.apparentTemperature,
                    windSpeed = h.windSpeed,
                    humidity = h.humidity,
                    pressure = h.pressure,
                    rawJson = if (index == 0) raw else null
                )
            }
        }
        if (entities.isNotEmpty()) snapshotsDao.insertAll(entities)
    }

    private suspend fun fetchObservations(locationId: Long, lat: Double, lon: Double) {
        val response = openMeteoApi.forecast(
            latitude = lat,
            longitude = lon,
            hourly = "temperature_2m,precipitation,weather_code",
            pastDays = 1
        )
        val h = response.hourly
        val offset = response.utcOffsetSeconds
        val cutoff = Instant.now().minus(1, ChronoUnit.HOURS)
        val observations = h.time.indices.mapNotNull { i ->
            val time = TimeParsers.openMeteoTime(h.time[i], offset)
            if (!time.isBefore(cutoff)) return@mapNotNull null
            val mm = h.precipitation.getOrNull(i) ?: 0.0
            ObservationEntity(
                locationId = locationId,
                targetHour = time.toEpochMilli(),
                hadPrecipitation = mm >= 0.1,
                precipType = if (mm >= 0.1) PrecipMappers.fromWmoCode(h.weather_code.getOrNull(i)).name else PrecipType.NONE.name,
                precipIntensity = mm,
                temperature = h.temperature_2m.getOrNull(i),
                source = "open-meteo-analysis",
                recordedAt = Instant.now().toEpochMilli()
            )
        }
        if (observations.isNotEmpty()) observationsDao.upsertAll(observations)
    }

    private suspend fun aggregate(location: SavedLocation, now: Instant): AggregatedForecast {
        val since = now.minus(12, ChronoUnit.HOURS).toEpochMilli()
        val recent = snapshotsDao.recent(location.id, since)
        val latestPerSource = recent.groupBy { it.sourceId }
            .mapValues { (_, rows) -> rows.maxOf { it.issuedAt } }
            .mapValues { (sourceId, issued) ->
                recent.filter { it.sourceId == sourceId && it.issuedAt == issued }
            }

        val observationCount = sourceMetricsDao.observationCount()
        val metricRows = sourceMetricsDao.forLocation(location.id)
        val brierPerSource = latestPerSource.keys.associateWith { sourceId ->
            val rows = metricRows.filter { it.sourceId == sourceId && !it.brier.isNaN() && it.sampleCount > 0 }
            if (rows.isEmpty()) Double.NaN
            else rows.minByOrNull { it.leadBucket }!!.brier
        }
        val weightResult = weightCalculator.compute(brierPerSource, observationCount)

        val forecasts = latestPerSource.mapNotNull { (sourceId, rows) ->
            val weight = weightResult.weights[sourceId] ?: return@mapNotNull null
            val providerForecast = ru.rufp.weather.domain.model.ProviderForecast(
                sourceId = sourceId,
                issuedAt = Instant.ofEpochMilli(rows.first().issuedAt),
                hours = rows.map {
                    ru.rufp.weather.domain.model.HourPoint(
                        time = Instant.ofEpochMilli(it.targetHour),
                        precipProbability = it.precipProbability,
                        precipIntensity = it.precipIntensity,
                        precipType = it.precipType?.let { t -> runCatching { PrecipType.valueOf(t) }.getOrNull() },
                        temperature = it.temperature,
                        apparentTemperature = it.apparentTemperature ?: it.temperature,
                        windSpeed = it.windSpeed,
                        humidity = it.humidity,
                        pressure = it.pressure
                    )
                }
            )
            weight to providerForecast
        }

        val result = aggregator.aggregate(
            location = GeoPoint(location.latitude, location.longitude),
            locationName = location.name,
            issuedAt = now,
            weightedForecasts = forecasts
        )
        return result.copy(location = location)
    }

    override suspend fun recomputeMetrics() {
        val location = locationRepository.currentLocationOnce() ?: return
        val now = Instant.now()
        val from = now.minus(30, ChronoUnit.DAYS).toEpochMilli()
        val snapshots = snapshotsDao.forRange(location.id, from, now.toEpochMilli() + 1)
        val observations = observationsDao.forRange(location.id, from, now.toEpochMilli() + 1)
        val obsByHour = observations.associateBy { it.targetHour }

        val samples = snapshots.groupBy { it.sourceId }.mapValues { (_, rows) ->
            rows.mapNotNull { snap ->
                val obs = obsByHour[snap.targetHour] ?: return@mapNotNull null
                val lead = ChronoUnit.HOURS.between(
                    Instant.ofEpochMilli(snap.issuedAt), Instant.ofEpochMilli(snap.targetHour)
                )
                if (lead < 1 || lead > 24) return@mapNotNull null
                val ageDays = ChronoUnit.MINUTES.between(
                    Instant.ofEpochMilli(snap.targetHour), now
                ) / 1440.0
                WeightedSample(
                    probability = snap.precipProbability ?: 0.05,
                    actual = obs.hadPrecipitation,
                    ageDays = ageDays,
                    leadHours = lead.toInt(),
                    temperatureError = if (snap.temperature != null && obs.temperature != null) {
                        kotlin.math.abs(snap.temperature - obs.temperature)
                    } else null,
                    forecastType = snap.precipType,
                    actualType = obs.precipType
                )
            }
        }

        val entities = samples.flatMap { (sourceId, samplesList) ->
            samplesList.groupBy { Metrics.leadBucket(it.leadHours) }.map { (bucket, bucketSamples) ->
                SourceMetricEntity(
                    sourceId = sourceId,
                    locationId = location.id,
                    leadBucket = bucket,
                    brier = Metrics.brier(bucketSamples),
                    maeTemperature = Metrics.mae(bucketSamples.mapNotNull { it.temperatureError }),
                    hitRate = Metrics.hitRate(bucketSamples),
                    falseAlarmRate = Metrics.falseAlarmRate(bucketSamples),
                    csi = Metrics.csi(bucketSamples),
                    sampleCount = bucketSamples.size,
                    updatedAt = now.toEpochMilli()
                )
            }
        }
        sourceMetricsDao.clearForLocation(location.id)
        if (entities.isNotEmpty()) sourceMetricsDao.upsertAll(entities)
    }

    override suspend fun buildSourceRows(): List<SourceRow> {
        val location = locationRepository.currentLocationOnce() ?: return emptyList()
        val now = Instant.now()
        val states = sourceStateDao.allOnce().associateBy { it.sourceId }
        val metricRows = sourceMetricsDao.forLocation(location.id)
        val recent = snapshotsDao.recent(location.id, now.minus(12, ChronoUnit.HOURS).toEpochMilli())
        val observationCount = sourceMetricsDao.observationCount()

        val latestBySource = recent.groupBy { it.sourceId }
            .mapValues { (_, rows) -> rows.filter { it.issuedAt == rows.maxOf { s -> s.issuedAt } } }

        val brierPerSource = latestBySource.keys.associateWith { sourceId ->
            val rows = metricRows.filter { it.sourceId == sourceId && !it.brier.isNaN() && it.sampleCount > 0 }
            if (rows.isEmpty()) Double.NaN else rows.minByOrNull { it.leadBucket }!!.brier
        }
        val weightResult = weightCalculator.compute(brierPerSource, observationCount)

        return registry.all.map { provider ->
            val state = states[provider.id]
            val enabled = state?.enabled ?: true
            val keyPresent = !provider.requiresKey || !apiKeyFor(provider.id).isNullOrBlank()
            val rows = latestBySource[provider.id] ?: emptyList()
            val lastSuccess = state?.lastSuccessAt
            val status = when {
                rows.isEmpty() && lastSuccess == null -> SourceStatus.ERROR
                rows.isEmpty() -> SourceStatus.STALE
                lastSuccess != null && now.toEpochMilli() - lastSuccess > 6 * 3600 * 1000L -> SourceStatus.STALE
                else -> SourceStatus.OK
            }
            val sourceMetrics = metricRows.filter { it.sourceId == provider.id }
            SourceRow(
                id = provider.id,
                displayName = provider.displayName,
                enabled = enabled,
                requiresKey = provider.requiresKey,
                keyPresent = keyPresent,
                status = status,
                lastSuccessAt = lastSuccess?.let { Instant.ofEpochMilli(it) },
                weight = weightResult.weights[provider.id] ?: 0.0,
                warmUp = weightResult.warmUp,
                brier = brierPerSource[provider.id] ?: Double.NaN,
                maeTemperature = Metrics.mae(sourceMetrics.mapNotNull { m ->
                    if (m.maeTemperature.isNaN()) null else m.maeTemperature
                }),
                hitRate = meanOf(sourceMetrics.mapNotNull { m ->
                    if (m.hitRate.isNaN()) null else m.hitRate
                }),
                nextHours = rows.sortedBy { it.targetHour }.map {
                    Instant.ofEpochMilli(it.targetHour) to (it.precipProbability ?: 0.0)
                }
            )
        }
    }

    private fun meanOf(values: List<Double>): Double =
        if (values.isEmpty()) Double.NaN else values.average()

    private fun apiKeyFor(sourceId: String): String? = when (sourceId) {
        "openweathermap" -> BuildConfig.OWM_KEY.takeIf { it.isNotBlank() }
        "weatherapi-com" -> BuildConfig.WEATHERAPI_KEY.takeIf { it.isNotBlank() }
        else -> null
    }
}

private fun AggregatedForecast.toCached(): CachedForecast = CachedForecast(
    locationId = location.id,
    locationName = location.name,
    issuedAtEpochMillis = issuedAt.toEpochMilli(),
    intervalStart = interval?.start?.toEpochMilli(),
    intervalEnd = interval?.end?.toEpochMilli(),
    intervalPeakTime = interval?.peakTime?.toEpochMilli(),
    intervalPeakProbability = interval?.peakProbability,
    intervalType = interval?.type?.name,
    sourcesDisagree = sourcesDisagree,
    hours = hours.map {
        CachedHour(
            timeEpochMillis = it.time.toEpochMilli(),
            probability = it.probability,
            minProbability = it.minProbability,
            maxProbability = it.maxProbability,
            spread = it.spread,
            intensity = it.intensity,
            type = it.type.name,
            temperature = it.temperature,
            apparentTemperature = it.apparentTemperature,
            windSpeed = it.windSpeed,
            humidity = it.humidity,
            pressure = it.pressure
        )
    }
)
