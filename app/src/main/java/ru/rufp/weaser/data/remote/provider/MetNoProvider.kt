package ru.rufp.weaser.data.remote.provider

import ru.rufp.weaser.data.remote.MetNoApi
import ru.rufp.weaser.data.remote.PrecipMappers
import ru.rufp.weaser.data.remote.TimeParsers
import ru.rufp.weaser.domain.model.GeoPoint
import ru.rufp.weaser.domain.model.HourPoint
import ru.rufp.weaser.domain.model.ProviderForecast
import ru.rufp.weaser.domain.provider.WeatherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetNoProvider @Inject constructor(
    private val api: MetNoApi
) : WeatherProvider {

    override val id: String = "met-no"
    override val displayName: String = "MET Norway"
    override val requiresKey: Boolean = false

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        val response = api.compact(location.latitude, location.longitude)
        val now = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val limit = now.plus(24, ChronoUnit.HOURS)
        val hours = response.properties.timeseries.mapNotNull { ts ->
            val time = TimeParsers.metNoTime(ts.time)
            if (time.isBefore(now) || time.isAfter(limit)) return@mapNotNull null
            val n1 = ts.data.next_1_hours
            val instant = ts.data.instant.details
            HourPoint(
                time = time,
                precipProbability = n1?.details?.probability_of_precipitation,
                precipIntensity = n1?.details?.precipitation_amount,
                precipType = PrecipMappers.fromMetSymbol(n1?.summary?.symbol_code),
                temperature = instant.air_temperature,
                apparentTemperature = instant.apparent_temperature ?: instant.air_temperature,
                windSpeed = instant.wind_speed,
                humidity = instant.relative_humidity,
                pressure = instant.air_pressure_at_sea_level?.times(0.750062)
            )
        }
        return ProviderForecast(id, Instant.now(), hours)
    }
}
