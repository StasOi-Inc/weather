package ru.rufp.weather.update

import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256Url: String?,
    val sizeBytes: Long
)

data class CheckResult(val update: UpdateInfo?, val networkOk: Boolean)

@Singleton
class UpdateChecker @Inject constructor(
    private val api: GitHubReleaseApi
) {

    suspend fun check(currentVersionCode: Long): CheckResult {
        val release = try {
            api.latest()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return CheckResult(update = null, networkOk = false)
        }
        val info = parseRelease(release)?.takeIf { it.versionCode > currentVersionCode }
        return CheckResult(update = info, networkOk = true)
    }

    companion object {
        private val APK_REGEX = Regex("^Weather-(v[0-9.]+)-(\\d+)-release\\.apk$")

        fun parseRelease(release: GitHubRelease): UpdateInfo? {
            val apk = release.assets.firstOrNull { APK_REGEX.matches(it.name) } ?: return null
            val sha = release.assets.firstOrNull { it.name == "${apk.name}.sha256" }
            val match = APK_REGEX.matchEntire(apk.name) ?: return null
            val (tag, codeStr) = match.destructured
            val versionCode = codeStr.toLongOrNull() ?: return null
            return UpdateInfo(
                versionName = tag.removePrefix("v"),
                versionCode = versionCode,
                apkUrl = apk.browser_download_url,
                sha256Url = sha?.browser_download_url,
                sizeBytes = apk.size
            )
        }
    }
}
