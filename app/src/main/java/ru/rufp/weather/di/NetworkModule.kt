package ru.rufp.weather.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ru.rufp.weather.BuildConfig
import ru.rufp.weather.data.remote.MetNoApi
import ru.rufp.weather.data.remote.OpenMeteoApi
import ru.rufp.weather.data.remote.OpenMeteoGeocodingApi
import ru.rufp.weather.data.remote.RainViewerApi
import ru.rufp.weather.data.remote.WeatherApiComApi
import ru.rufp.weather.data.remote.OpenWeatherApi
import ru.rufp.weather.data.remote.YandexWeatherApi
import ru.rufp.weather.data.remote.provider.GismeteoProvider
import ru.rufp.weather.data.remote.provider.MetNoProvider
import ru.rufp.weather.data.remote.provider.OpenMeteoProvider
import ru.rufp.weather.data.remote.provider.OpenWeatherProvider
import ru.rufp.weather.data.remote.provider.WeatherApiComProvider
import ru.rufp.weather.data.remote.provider.YandexProvider
import ru.rufp.weather.domain.provider.ProviderRegistry
import ru.rufp.weather.domain.provider.WeatherProvider
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier annotation class OpenMeteoRetrofit
@Qualifier annotation class GeocodingRetrofit
@Qualifier annotation class MetNoRetrofit
@Qualifier annotation class RainViewerRetrofit
@Qualifier annotation class OpenWeatherRetrofit
@Qualifier annotation class WeatherApiRetrofit
@Qualifier annotation class YandexRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val USER_AGENT = "Weather/1.0 (Android; RUFP Desktop project)"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private fun retrofit(client: OkHttpClient, json: Json, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @OpenMeteoRetrofit
    fun provideOpenMeteoRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.open-meteo.com/")

    @Provides
    @Singleton
    @GeocodingRetrofit
    fun provideGeocodingRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://geocoding-api.open-meteo.com/")

    @Provides
    @Singleton
    @MetNoRetrofit
    fun provideMetNoRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.met.no/")

    @Provides
    @Singleton
    @RainViewerRetrofit
    fun provideRainViewerRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.rainviewer.com/")

    @Provides
    @Singleton
    @OpenWeatherRetrofit
    fun provideOpenWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.openweathermap.org/")

    @Provides
    @Singleton
    @WeatherApiRetrofit
    fun provideWeatherApiRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.weatherapi.com/")

    @Provides
    @Singleton
    @YandexRetrofit
    fun provideYandexRetrofit(client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json, "https://api.weather.yandex.ru/")

    @Provides
    @Singleton
    fun provideOpenMeteoApi(@OpenMeteoRetrofit retrofit: Retrofit): OpenMeteoApi =
        retrofit.create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    fun provideGeocodingApi(@GeocodingRetrofit retrofit: Retrofit): OpenMeteoGeocodingApi =
        retrofit.create(OpenMeteoGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideMetNoApi(@MetNoRetrofit retrofit: Retrofit): MetNoApi =
        retrofit.create(MetNoApi::class.java)

    @Provides
    @Singleton
    fun provideRainViewerApi(@RainViewerRetrofit retrofit: Retrofit): RainViewerApi =
        retrofit.create(RainViewerApi::class.java)

    @Provides
    @Singleton
    fun provideOpenWeatherApi(@OpenWeatherRetrofit retrofit: Retrofit): OpenWeatherApi =
        retrofit.create(OpenWeatherApi::class.java)

    @Provides
    @Singleton
    fun provideWeatherApiComApi(@WeatherApiRetrofit retrofit: Retrofit): WeatherApiComApi =
        retrofit.create(WeatherApiComApi::class.java)

    @Provides
    @Singleton
    fun provideYandexApi(@YandexRetrofit retrofit: Retrofit): YandexWeatherApi =
        retrofit.create(YandexWeatherApi::class.java)

    @Provides
    @Singleton
    fun provideProviderRegistry(
        openMeteo: OpenMeteoProvider,
        metNo: MetNoProvider,
        openWeather: OpenWeatherProvider,
        weatherApiCom: WeatherApiComProvider,
        yandex: YandexProvider,
        gismeteo: GismeteoProvider
    ): ProviderRegistry = ProviderRegistry(
        listOf<WeatherProvider>(
            openMeteo, metNo, openWeather, weatherApiCom, yandex, gismeteo
        )
    )

    val totalSyncTimeout: Duration = Duration.ofSeconds(10)
}
