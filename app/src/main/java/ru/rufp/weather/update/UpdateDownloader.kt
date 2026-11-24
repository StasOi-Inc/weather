package ru.rufp.weather.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateDownloader @Inject constructor(
    private val client: OkHttpClient,
    private val storage: UpdateStorage
) {

    fun download(url: String, sha256Url: String?): Flow<Int> = flow {
        val expectedSha = sha256Url?.let { fetchSha256(it) }
        val target = storage.downloadFile()
        target.delete()
        val temp = File(target.parentFile, target.name + ".part")

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = requireNotNull(response.body) { "Empty body" }
            val total = body.contentLength()
            val digest = MessageDigest.getInstance("SHA-256")
            body.byteStream().use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            emit((done * 100 / total).toInt().coerceIn(0, 99))
                        }
                    }
                }
            }
        }
        if (expectedSha != null) {
            val actual = digestOf(temp)
            check(actual.equals(expectedSha, ignoreCase = true)) { "SHA-256 mismatch" }
        }
        check(temp.renameTo(target)) { "Rename failed" }
        emit(100)
    }.flowOn(Dispatchers.IO)

    private fun fetchSha256(url: String): String? = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.length == 64 }
            } else null
        }
    } catch (e: Exception) {
        null
    }

    private fun digestOf(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
