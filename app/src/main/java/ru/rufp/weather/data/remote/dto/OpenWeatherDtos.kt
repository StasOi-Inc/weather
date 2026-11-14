package ru.rufp.weather.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherItem> = emptyList()
)

@Serializable
data class OpenWeatherItem(
    val dt: Long = 0,
    val main: OpenWeatherMain = OpenWeatherMain(),
    val wind: OpenWeatherWind = OpenWeatherWind(),
    val pop: Double? = null,
    val rain: Map<String, Double>? = null,
    val snow: Map<String, Double>? = null,
    val weather: List<OpenWeatherCond> = emptyList()
)

@Serializable
data class OpenWeatherMain(
    val temp: Double? = null,
    val feels_like: Double? = null,
    val humidity: Int? = null,
    val pressure: Int? = null
)

@Serializable
data class OpenWeatherWind(
    val speed: Double? = null
)

@Serializable
data class OpenWeatherCond(
    val id: Int = 0
)
