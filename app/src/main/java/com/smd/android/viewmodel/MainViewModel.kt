package com.smd.android.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smd.android.model.*
import com.smd.android.network.SteamApiClient
import com.smd.android.repository.HistoryRepository
import com.smd.android.repository.SettingsRepository
import com.smd.android.service.DownloadService
import com.smd.android.util.FormatUtil
import com.smd.android.util.LuaParser
import com.smd.android.util.StParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val historyRepo = HistoryRepository(application)
    private val gson = Gson()

    // ─── Settings ─────────────────────────────────────────────────────────

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun saveSettings(s: AppSettings) = viewModelScope.launch {
        settingsRepo.saveSettings(s)
        SteamApiClient.updateClient(s.proxyUrl)
    }

    // ─── History & Queue ──────────────────────────────────────────────────

    val history: StateFlow<List<HistoryEntry>> = historyRepo.historyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val queue: StateFlow<List<QueueEntry>> = historyRepo.queueFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun removeHistory(id: String) = viewModelScope.launch { historyRepo.removeHistory(id) }
    fun removeFromQueue(id: String) = viewModelScope.launch { historyRepo.removeFromQueue(id) }
    fun clearQueue() = viewModelScope.launch { historyRepo.clearQueue() }

    // ─── Upload / Parse ───────────────────────────────────────────────────

    private val _parsedManifest = MutableStateFlow<ParsedManifest?>(null)
    val parsedManifest: StateFlow<ParsedManifest?> = _parsedManifest

    private val _parseError = MutableStateFlow<String?>(null)
    val parseError: StateFlow<String?> = _parseError

    private val _isLoadingManifest = MutableStateFlow(false)
    val isLoadingManifest: StateFlow<Boolean> = _isLoadingManifest

    fun parseFile(uri: Uri) {
        viewModelScope.launch {
            _isLoadingManifest.value = true
            _parseError.value = null
            val ctx = getApplication<Application>()
            val fileName = uri.lastPathSegment ?: ""

            val manifest = when {
                fileName.endsWith(".lua", ignoreCase = true) ->
                    LuaParser.parseFromUri(ctx, uri)
                fileName.endsWith(".st", ignoreCase = true) ->
                    StParser.parseFromUri(ctx, uri)
                else -> {
                    // Try lua first, then .st
                    LuaParser.parseFromUri(ctx, uri) ?: StParser.parseFromUri(ctx, uri)
                }
            }

            if (manifest != null) {
                // Enrich with game info and depot PICS metadata
                enrichManifest(manifest)
            } else {
                _parseError.value = "Could not parse file. Expected .lua or .st format."
                _isLoadingManifest.value = false
            }
        }
    }

    // ─── Search by App ID ─────────────────────────────────────────────────

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun searchGames(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val results = SteamApiClient.searchGames(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    private val _selectedSourceId = MutableStateFlow<String?>(null)
    val selectedSourceId: StateFlow<String?> = _selectedSourceId

    fun setSelectedSourceId(id: String?) {
        _selectedSourceId.value = id
    }

    fun searchByAppId(appId: Long, customSourceId: String? = null) {
        viewModelScope.launch {
            _isLoadingManifest.value = true
            _parseError.value = null
            val s = settings.value
            val targetSource = customSourceId ?: _selectedSourceId.value

            val manifest = SteamApiClient.findManifestByAppId(
                appId = appId,
                sources = s.sources,
                manifesthubKey = s.manifesthubApiKey,
                hubcapKey = s.hubcapApiKey,
                ryuuKey = s.ryuuApiKey,
                targetSourceId = targetSource
            )

            if (manifest != null) {
                enrichManifest(manifest)
            } else {
                val serverName = if (!targetSource.isNullOrBlank()) {
                    s.sources.firstOrNull { it.id == targetSource }?.name ?: targetSource
                } else null

                _parseError.value = if (serverName != null) {
                    "No depot information found for AppID $appId on server: $serverName"
                } else {
                    "No depot information found for AppID $appId in any configured source."
                }
                _isLoadingManifest.value = false
            }
        }
    }

    private suspend fun enrichManifest(manifest: ParsedManifest) {
        // Fetch game name & cover from Steam Store API
        val (name, coverUrl) = SteamApiClient.fetchGameInfo(manifest.appId)
        // Fetch depot PICS info (OS, arch, language tags)
        val depotInfos = SteamApiClient.fetchDepotInfos(manifest.appId)
        val infoMap = depotInfos.associateBy { it.depotId }

        val enrichedDepots = manifest.depots.map { depot ->
            val info = infoMap[depot.depotId]
            val os = info?.config?.oslist
            val arch = info?.config?.osarch?.let { if (it == "64") "64-bit" else "32-bit" }
            val lang = info?.config?.language
            depot.copy(
                name = info?.name ?: depot.name,
                osArch = listOfNotNull(os, arch).joinToString("/").ifBlank { null },
                language = lang ?: depot.language,
                sizeBytes = info?.maxSize ?: depot.sizeBytes
            )
        }

        _parsedManifest.value = manifest.copy(
            appName = name ?: manifest.appName ?: "App ${manifest.appId}",
            depots = enrichedDepots
        )
        _gameArtUrl.value = coverUrl
        _isLoadingManifest.value = false
    }

    // ─── Game art ─────────────────────────────────────────────────────────

    private val _gameArtUrl = MutableStateFlow<String?>(null)
    val gameArtUrl: StateFlow<String?> = _gameArtUrl

    // ─── Depot selection ──────────────────────────────────────────────────

    fun toggleDepot(depotId: Long) {
        val current = _parsedManifest.value ?: return
        _parsedManifest.value = current.copy(
            depots = current.depots.map { d ->
                if (d.depotId == depotId) d.copy(isSelected = !d.isSelected) else d
            }
        )
    }

    fun selectAllDepots(select: Boolean) {
        val current = _parsedManifest.value ?: return
        _parsedManifest.value = current.copy(
            depots = current.depots.map { it.copy(isSelected = select) }
        )
    }

    /** Auto-select depots matching platform/language (Like Steam mode) */
    fun autoSelectDepots(platform: String = "android", language: String = "english") {
        val current = _parsedManifest.value ?: return
        _parsedManifest.value = current.copy(
            depots = current.depots.map { d ->
                val osMatch = d.osArch == null || d.osArch.contains(platform, ignoreCase = true)
                    || d.osArch.contains("windows", ignoreCase = true) // include windows depots
                val langMatch = d.language == null
                    || d.language.equals(language, ignoreCase = true)
                    || d.language.equals("english", ignoreCase = true)
                d.copy(isSelected = osMatch && langMatch)
            }
        )
    }

    // ─── Download ─────────────────────────────────────────────────────────

    val downloadProgress: StateFlow<OverallProgress> = DownloadService.progressFlow
    val downloadLog: StateFlow<List<String>> = DownloadService.logFlow
    val isDownloadRunning: StateFlow<Boolean> = DownloadService.isRunning

    fun startDownload(outputPath: String = "") {
        val manifest = _parsedManifest.value ?: return
        val selected = manifest.depots.filter { it.isSelected }
        if (selected.isEmpty()) return

        val ctx = getApplication<Application>()
        val finalOutput = outputPath.ifBlank {
            settings.value.outputDirectory.ifBlank {
                ctx.getExternalFilesDir(null)?.absolutePath
                    ?: ctx.filesDir.absolutePath
            }
        } + "/${manifest.appName?.replace("[^\\w\\s-]".toRegex(), "") ?: manifest.appId}"

        val intent = Intent(ctx, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_APP_ID, manifest.appId)
            putExtra(DownloadService.EXTRA_APP_NAME, manifest.appName ?: "App ${manifest.appId}")
            putExtra(DownloadService.EXTRA_OUTPUT_PATH, finalOutput)
            putExtra(DownloadService.EXTRA_DEPOTS_JSON, gson.toJson(selected))
            putExtra(DownloadService.EXTRA_SPEED_LIMIT, settings.value.speedLimitBytesPerSec)
        }
        ctx.startForegroundService(intent)

        // Add to history
        viewModelScope.launch {
            val entry = HistoryEntry(
                id = UUID.randomUUID().toString(),
                appId = manifest.appId,
                appName = manifest.appName ?: "App ${manifest.appId}",
                coverUrl = _gameArtUrl.value,
                downloadPath = finalOutput,
                status = DownloadStatus.DOWNLOADING,
                downloadedBytes = 0L,
                totalBytes = selected.sumOf { it.sizeBytes ?: 0L },
                downloadedAt = System.currentTimeMillis(),
                selectedDepots = selected.map { it.depotId },
                serverUsed = manifest.sourceName
            )
            historyRepo.addOrUpdateHistory(entry)
        }
    }

    fun pauseDownload() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE
        })
    }

    fun resumeDownload() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
        })
    }

    fun cancelDownload() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
        })
    }

    // ─── Queue ────────────────────────────────────────────────────────────

    fun addToQueue() {
        val manifest = _parsedManifest.value ?: return
        val selected = manifest.depots.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val outputPath = settings.value.outputDirectory.ifBlank {
                ctx.getExternalFilesDir(null)?.absolutePath ?: ctx.filesDir.absolutePath
            }
            historyRepo.addToQueue(QueueEntry(
                id = UUID.randomUUID().toString(),
                manifest = manifest,
                selectedDepots = selected.map { it.depotId },
                outputPath = outputPath
            ))
        }
    }

    fun clearParsedManifest() {
        _parsedManifest.value = null
        _parseError.value = null
        _gameArtUrl.value = null
        _searchResults.value = emptyList()
    }
}
