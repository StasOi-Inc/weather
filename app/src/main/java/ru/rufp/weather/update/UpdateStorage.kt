package ru.rufp.weather.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.UpdateDataStore by preferencesDataStore(name = "update_state")

@Singleton
class UpdateStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val infoKey = stringPreferencesKey("available_update")
    private val checkedAtKey = longPreferencesKey("checked_at")

    fun available(): Flow<UpdateInfo?> = context.UpdateDataStore.data.map { prefs ->
        prefs[infoKey]?.let { raw ->
            runCatching { json.decodeFromString<UpdateInfo>(raw) }.getOrNull()
        }
    }

    suspend fun save(update: UpdateInfo) {
        context.UpdateDataStore.edit { prefs ->
            prefs[infoKey] = json.encodeToString(update)
            prefs[checkedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.UpdateDataStore.edit { prefs ->
            prefs.remove(infoKey)
            prefs[checkedAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun lastCheckedAt(): Long = context.UpdateDataStore.data.first()[checkedAtKey] ?: 0L

    fun downloadFile(): File = File(context.cacheDir, "update/Weather-update.apk").also {
        it.parentFile?.mkdirs()
    }
}
