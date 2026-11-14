package ru.rufp.weaser.data.remote

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import ru.rufp.weaser.data.remote.dto.MetNoResponse

interface MetNoApi {

    @Headers("Accept: application/json")
    @GET("weatherapi/locationforecast/2.0/compact")
    suspend fun compact(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): MetNoResponse
}

interface RainViewerApi {

    @GET("public/weather-maps.json")
    suspend fun weatherMaps(): ru.rufp.weaser.data.remote.dto.RainViewerMaps
}
