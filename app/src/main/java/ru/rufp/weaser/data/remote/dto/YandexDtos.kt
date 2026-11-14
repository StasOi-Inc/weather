package ru.rufp.weaser.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YandexForecastResponse(
    val forecasts: List<YandexForecastDay> = emptyList()
)

@Serializable
data class YandexForecastDay(
    val date: String = "",
    val hours: List<YandexHour> = emptyList()
)

@Serializable
data class YandexHour(
    val hour: Int = 0,
    val temp: Double? = null,
    val feels_like: Double? = null,
    val condition: String = "",
    val prec_mm: Double? = null,
    val prec_prob: Int? = null,
    val prec_type: Int? = null,
    val wind_speed: Double? = null,
    val humidity: Double? = null,
    val pressure_mm: Double? = null
)
