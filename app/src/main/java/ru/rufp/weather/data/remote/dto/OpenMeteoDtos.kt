package ru.rufp.weather.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val utcOffsetSeconds: Long = 0,
    val hourly: OpenMeteoHourly = OpenMeteoHourly()
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    val temperature_2m: List<Double?> = emptyList(),
    val apparent_temperature: List<Double?> = emptyList(),
    val precipitation_probability: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    val weather_code: List<Int?> = emptyList(),
    val relative_humidity_2m: List<Int?> = emptyList(),
    val wind_speed_10m: List<Double?> = emptyList(),
    val surface_pressure: List<Double?> = emptyList()
)

@Serializable
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult> = emptyList()
)

@Serializable
data class OpenMeteoGeocodingResult(
    val id: Long = 0,
    val name: String = "",
    val country: String? = null,
    val admin1: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
