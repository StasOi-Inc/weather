package ru.rufp.weather.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {

    private fun asset(name: String, size: Long = 10_000_000) =
        GitHubAsset(name = name, size = size, browser_download_url = "https://example.com/$name")

    private fun release(vararg names: String) =
        GitHubRelease(tag_name = "v1.2", assets = names.map { asset(it) })

    @Test
    fun parsesValidReleaseAsset() {
        val info = UpdateChecker.parseRelease(release("Weather-v1.2-3-release.apk"))
        assertNotNull(info)
        assertEquals("1.2", info!!.versionName)
        assertEquals(3L, info.versionCode)
        assertEquals("https://example.com/Weather-v1.2-3-release.apk", info.apkUrl)
        assertEquals(10_000_000L, info.sizeBytes)
    }

    @Test
    fun parsesSha256Asset() {
        val info = UpdateChecker.parseRelease(
            release("Weather-v1.2-3-release.apk", "Weather-v1.2-3-release.apk.sha256")
        )
        assertNotNull(info)
        assertEquals("https://example.com/Weather-v1.2-3-release.apk.sha256", info!!.sha256Url)
    }

    @Test
    fun ignoresUnrelatedAssets() {
        assertNull(UpdateChecker.parseRelease(release("random.apk", "Weather-notes.txt")))
    }

    @Test
    fun wrongVersionNameIgnored() {
        assertNull(UpdateChecker.parseRelease(release("Weather-beta-3-release.apk")))
    }

    @Test
    fun nonNumericCodeIgnored() {
        assertNull(UpdateChecker.parseRelease(release("Weather-v1.2-x-release.apk")))
    }
}
