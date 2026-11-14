package ru.rufp.weaser.domain.provider

import ru.rufp.weaser.domain.model.GeoPoint
import ru.rufp.weaser.domain.model.ProviderForecast

interface WeatherProvider {
    val id: String
    val displayName: String
    val requiresKey: Boolean

    suspend fun fetch(location: GeoPoint, apiKey: String?): ProviderForecast
}

class ProviderRegistry(private val providers: List<WeatherProvider>) {
    val all: List<WeatherProvider> get() = providers

    fun byId(id: String): WeatherProvider? = providers.firstOrNull { it.id == id }
}
