package ru.rufp.weaser.data.remote.provider

import ru.rufp.weaser.data.remote.PrecipMappers
import ru.rufp.weaser.data.remote.TimeParsers
import ru.rufp.weaser.data.remote.YandexWeatherApi
import ru.rufp.weaser.domain.model.GeoPoint
import ru.rufp.weaser.domain.model.HourPoint
import ru.rufp.weaser.domain.model.ProviderForecast
import ru.rufp.weaser.domain.provider.WeatherProvider
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YandexProvider @Inject constructor(
    private val api: YandexWeatherApi
) : WeatherProvider {

    override val id: String = "yandex"
    override val displayName: String = "Яндекс Погода"
    override val requiresKey: Boolean = true

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        val key = requireNotNull(apiKey?.takeIf { it.isNotBlank() }) { "Yandex Weather API key required" }
        val response = api.forecast(location.latitude, location.longitude)
        val zone = ZoneId.systemDefault()
        val now = Instant.now().truncatedTo(ChronoUnit.HOURS)
        val limit = now.plus(24, ChronoUnit.HOURS)
        val hours = response.forecasts.flatMap { day ->
            day.hours.mapNotNull { h ->
                val time = TimeParsers.yandexTime(day.date, h.hour, zone)
                if (time.isBefore(now) || time.isAfter(limit)) return@mapNotNull null
                HourPoint(
                    time = time,
                    precipProbability = h.prec_prob?.div(100.0),
                    precipIntensity = h.prec_mm,
                    precipType = PrecipMappers.fromYandexType(h.prec_type, h.temp),
                    temperature = h.temp,
                    apparentTemperature = h.feels_like,
                    windSpeed = h.wind_speed,
                    humidity = h.humidity,
                    pressure = h.pressure_mm
                )
            }
        }
        return ProviderForecast(id, Instant.now(), hours)
    }
}
