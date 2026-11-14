package ru.rufp.weaser.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import ru.rufp.weaser.data.remote.dto.OpenMeteoForecastResponse
import ru.rufp.weaser.data.remote.dto.OpenMeteoGeocodingResponse

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String,
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("past_days") pastDays: Int = 0,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "ru",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}
