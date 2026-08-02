package com.stopwatch.ftc.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stopwatch.ftc.domain.StopwatchSnapshot
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.stopwatchDataStore by preferencesDataStore(name = "stopwatch")

/**
 * Persists the stopwatch across process death. Backed by DataStore, so every read and write happens
 * off the main thread — a plain SharedPreferences load would block the first frame on disk I/O.
 */
class StopwatchStore(context: Context) {

    private val dataStore = context.applicationContext.stopwatchDataStore

    /** Returns the stored snapshot, or `null` if there is nothing usable saved. */
    suspend fun load(): StopwatchSnapshot? {
        val raw =
            dataStore.data
                .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
                .first()[SNAPSHOT_KEY] ?: return null
        // A snapshot written by an older build may no longer parse. Starting fresh beats crashing.
        return runCatching { json.decodeFromString<StopwatchSnapshot>(raw) }.getOrNull()
    }

    suspend fun save(snapshot: StopwatchSnapshot) {
        // Losing a checkpoint is survivable; taking the app down with it is not.
        runCatching { dataStore.edit { it[SNAPSHOT_KEY] = json.encodeToString(snapshot) } }
    }

    private companion object {
        val SNAPSHOT_KEY = stringPreferencesKey("snapshot")
        val json = Json { ignoreUnknownKeys = true }
    }
}
