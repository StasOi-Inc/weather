package ru.rufp.weather.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import ru.rufp.weather.data.remote.dto.OpenWeatherForecastResponse
import ru.rufp.weather.data.remote.dto.WeatherApiForecastResponse

interface OpenWeatherApi {

    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherForecastResponse
}

interface WeatherApiComApi {

    @GET("v1/forecast.json")
    suspend fun forecast(
        @Query("key") key: String,
        @Query("q") q: String,
        @Query("days") days: Int = 2,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherApiForecastResponse
}
