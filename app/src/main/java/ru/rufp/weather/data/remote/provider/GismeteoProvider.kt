package ru.rufp.weather.data.remote.provider

import ru.rufp.weather.domain.model.GeoPoint
import ru.rufp.weather.domain.model.HourPoint
import ru.rufp.weather.domain.model.PrecipType
import ru.rufp.weather.domain.model.ProviderForecast
import ru.rufp.weather.domain.provider.WeatherProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GismeteoProvider @Inject constructor() : WeatherProvider {

    override val id: String = "gismeteo"
    override val displayName: String = "Гисметео"
    override val requiresKey: Boolean = true

    override suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast {
        throw UnsupportedOperationException(
            "Gismeteo API contract must be verified against official partner documentation before enabling"
        )
    }

    fun toHourPoint(
        time: Instant,
        prob: Double?,
        mm: Double?,
        type: PrecipType?,
        temp: Double?,
        feels: Double?,
        wind: Double?,
        humidity: Double?,
        pressure: Double?
    ): HourPoint = HourPoint(time, prob, mm, type, temp, feels, wind, humidity, pressure)
}
