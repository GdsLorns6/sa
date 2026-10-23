package com.smd.android.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smd.android.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_OUTPUT_DIR = stringPreferencesKey("output_dir")
        val KEY_SOURCES_JSON = stringPreferencesKey("sources_json")
        val KEY_SPEED_LIMIT = longPreferencesKey("speed_limit")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_SHUTDOWN_AFTER = booleanPreferencesKey("shutdown_after")
        val KEY_LIKE_STEAM = booleanPreferencesKey("like_steam")
        val KEY_MANIFESTHUB_KEY = stringPreferencesKey("manifesthub_key")
        val KEY_HUBCAP_KEY = stringPreferencesKey("hubcap_key")
        val KEY_RYUU_KEY = stringPreferencesKey("ryuu_key")
        val KEY_PROXY_URL = stringPreferencesKey("proxy_url")
        val KEY_TELEMETRY = booleanPreferencesKey("telemetry")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val sourcesJson = prefs[KEY_SOURCES_JSON]
        val sources: List<DepotSource> = if (!sourcesJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<DepotSource>>() {}.type
                gson.fromJson(sourcesJson, type)
            } catch (e: Exception) {
                defaultSources()
            }
        } else {
            defaultSources()
        }

        AppSettings(
            outputDirectory = prefs[KEY_OUTPUT_DIR] ?: "",
            sources = sources,
            speedLimitBytesPerSec = prefs[KEY_SPEED_LIMIT] ?: 0L,
            theme = prefs[KEY_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DARK }
            } ?: AppTheme.DARK,
            language = prefs[KEY_LANGUAGE] ?: "en",
            shutdownAfterDownload = prefs[KEY_SHUTDOWN_AFTER] ?: false,
            likeSteamMode = prefs[KEY_LIKE_STEAM] ?: false,
            manifesthubApiKey = prefs[KEY_MANIFESTHUB_KEY] ?: "",
            hubcapApiKey = prefs[KEY_HUBCAP_KEY] ?: "",
            ryuuApiKey = prefs[KEY_RYUU_KEY] ?: "",
            proxyUrl = prefs[KEY_PROXY_URL] ?: "",
            telemetryEnabled = prefs[KEY_TELEMETRY] ?: false
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_OUTPUT_DIR] = settings.outputDirectory
            prefs[KEY_SOURCES_JSON] = gson.toJson(settings.sources)
            prefs[KEY_SPEED_LIMIT] = settings.speedLimitBytesPerSec
            prefs[KEY_THEME] = settings.theme.name
            prefs[KEY_LANGUAGE] = settings.language
            prefs[KEY_SHUTDOWN_AFTER] = settings.shutdownAfterDownload
            prefs[KEY_LIKE_STEAM] = settings.likeSteamMode
            prefs[KEY_MANIFESTHUB_KEY] = settings.manifesthubApiKey
            prefs[KEY_HUBCAP_KEY] = settings.hubcapApiKey
            prefs[KEY_RYUU_KEY] = settings.ryuuApiKey
            prefs[KEY_PROXY_URL] = settings.proxyUrl
            prefs[KEY_TELEMETRY] = settings.telemetryEnabled
        }
    }
}

class HistoryRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_HISTORY_JSON = stringPreferencesKey("history_json")
        val KEY_QUEUE_JSON = stringPreferencesKey("queue_json")
    }

    val historyFlow: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY_JSON] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<HistoryEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val queueFlow: Flow<List<QueueEntry>> = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_QUEUE_JSON] ?: return@map emptyList()
        try {
            val type = object : TypeToken<List<QueueEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addOrUpdateHistory(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = readHistory(prefs)
            val updated = current.filter { it.id != entry.id } + entry
            prefs[KEY_HISTORY_JSON] = gson.toJson(updated)
        }
    }

    suspend fun removeHistory(id: String) {
        context.historyDataStore.edit { prefs ->
            val current = readHistory(prefs)
            prefs[KEY_HISTORY_JSON] = gson.toJson(current.filter { it.id != id })
        }
    }

    suspend fun addToQueue(entry: QueueEntry) {
        context.historyDataStore.edit { prefs ->
            val current = readQueue(prefs)
            prefs[KEY_QUEUE_JSON] = gson.toJson(current + entry)
        }
    }

    suspend fun removeFromQueue(id: String) {
        context.historyDataStore.edit { prefs ->
            val current = readQueue(prefs)
            prefs[KEY_QUEUE_JSON] = gson.toJson(current.filter { it.id != id })
        }
    }

    suspend fun clearQueue() {
        context.historyDataStore.edit { prefs ->
            prefs[KEY_QUEUE_JSON] = "[]"
        }
    }

    private fun readHistory(prefs: Preferences): List<HistoryEntry> {
        val json = prefs[KEY_HISTORY_JSON] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readQueue(prefs: Preferences): List<QueueEntry> {
        val json = prefs[KEY_QUEUE_JSON] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QueueEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
