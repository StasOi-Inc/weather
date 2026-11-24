package ru.rufp.weather.data.remote.provider

import ru.rufp.weather.data.remote.OpenWeatherApi
import ru.rufp.weather.data.remote.PrecipMappers
import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.HourPoint
import ru.rufp.weather.domain.model.ProviderForecast
import ru.rufp.weather.domain.provider.WeatherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenWeatherProvider @Inject constructor(
    private val api: OpenWeatherApi
) : WeatherProvider {

    override val id: String = "openweathermap"
    override val displayName: String = "OpenWeatherMap"
    override val requiresKey: Boolean = true

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        val key = requireNotNull(apiKey?.takeIf { it.isNotBlank() }) { "OpenWeatherMap API key required" }
        val response = api.forecast(location.latitude, location.longitude, key)
        val now = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val limit = now.plus(24, ChronoUnit.HOURS)
        val threeHourly = response.list.mapNotNull { item ->
            val time = Instant.ofEpochSecond(item.dt)
            if (time.isBefore(now) || time.isAfter(limit)) return@mapNotNull null
            val rainMm = item.rain?.get("3h") ?: 0.0
            val snowMm = item.snow?.get("3h") ?: 0.0
            val mmPerHour = (rainMm + snowMm) / 3.0
            HourPoint(
                time = time,
                precipProbability = item.pop,
                precipIntensity = mmPerHour,
                precipType = PrecipMappers.fromOpenWeatherId(item.weather.firstOrNull()?.id),
                temperature = item.main.temp,
                apparentTemperature = item.main.feels_like,
                windSpeed = item.wind.speed,
                humidity = item.main.humidity?.toDouble(),
                pressure = item.main.pressure?.times(0.750062)
            )
        }
        val hours = ru.rufp.weather.data.remote.HourlyInterpolator.interpolateToHourly(threeHourly)
            .filter { !it.time.isBefore(now) && !it.time.isAfter(limit) }
        return ProviderForecast(id, Instant.now(), hours)
    }
}
