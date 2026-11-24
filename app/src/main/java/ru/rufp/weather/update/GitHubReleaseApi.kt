package ru.rufp.weather.update

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class GitHubAsset(
    val name: String = "",
    val size: Long = 0,
    val browser_download_url: String = ""
)

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

interface GitHubReleaseApi {

    @GET("repos/StasOi-Inc/weather/releases/latest")
    suspend fun latest(): GitHubRelease
}
