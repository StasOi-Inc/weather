package ru.rufp.weather.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiForecastResponse(
    val forecast: WeatherApiForecast = WeatherApiForecast()
)

@Serializable
data class WeatherApiForecast(
    val forecastday: List<WeatherApiForecastDay> = emptyList()
)

@Serializable
data class WeatherApiForecastDay(
    val hour: List<WeatherApiHour> = emptyList()
)

@Serializable
data class WeatherApiHour(
    val time_epoch: Long = 0,
    val temp_c: Double? = null,
    val feelslike_c: Double? = null,
    val humidity: Double? = null,
    val pressure_mb: Double? = null,
    val wind_kph: Double? = null,
    val precip_mm: Double? = null,
    val chance_of_rain: Int? = null,
    val chance_of_snow: Int? = null,
    val will_it_rain: Int? = null,
    val will_it_snow: Int? = null,
    val condition: WeatherApiCondition = WeatherApiCondition()
)

@Serializable
data class WeatherApiCondition(
    val code: Int = 0,
    val text: String = ""
)
