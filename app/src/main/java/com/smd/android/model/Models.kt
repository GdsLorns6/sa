package com.smd.android.model

import androidx.annotation.Keep

// ─── Depot / Manifest models ───────────────────────────────────────────────

@Keep
data class DepotEntry(
    val depotId: Long,
    val manifestId: Long?,
    val decryptionKey: String?,
    val name: String? = null,
    val osArch: String? = null,       // e.g. "windows/64", "linux/64", "macos/64"
    val language: String? = null,     // e.g. "english", "german"
    val sizeBytes: Long? = null,
    var isSelected: Boolean = false
)

@Keep
data class ParsedManifest(
    val appId: Long,
    val appName: String?,
    val depots: List<DepotEntry>,
    val sourceName: String = ""        // which server/source found this
)

// ─── Search result (from depot sources) ───────────────────────────────────

@Keep
data class SearchResult(
    val appId: Long,
    val appName: String,
    val coverUrl: String? = null,
    val availableSources: List<String> = emptyList()
)

// ─── Download models ───────────────────────────────────────────────────────

enum class DownloadStatus {
    IDLE, QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

@Keep
data class DownloadProgress(
    val depotId: Long,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val error: String? = null
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
}

@Keep
data class OverallProgress(
    val totalDepots: Int = 0,
    val completedDepots: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val currentLog: List<String> = emptyList()
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else
            if (totalDepots > 0) completedDepots.toFloat() / totalDepots.toFloat() else 0f
}

// ─── History model ────────────────────────────────────────────────────────

@Keep
data class HistoryEntry(
    val id: String,                    // UUID
    val appId: Long,
    val appName: String,
    val coverUrl: String? = null,
    val downloadPath: String,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadedAt: Long,            // epoch ms
    val selectedDepots: List<Long>,
    val serverUsed: String
)

// ─── Download queue ───────────────────────────────────────────────────────

@Keep
data class QueueEntry(
    val id: String,
    val manifest: ParsedManifest,
    val selectedDepots: List<Long>,
    val outputPath: String,
    val addedAt: Long = System.currentTimeMillis()
)

// ─── Steam PICS depot info ────────────────────────────────────────────────

@Keep
data class DepotInfo(
    val depotId: Long,
    val name: String?,
    val maxSize: Long?,
    val config: DepotConfig?
)

@Keep
data class DepotConfig(
    val oslist: String?,          // "windows", "linux", "macos"
    val osarch: String?,          // "32", "64"
    val language: String?
)

// ─── Server / depot source ────────────────────────────────────────────────

enum class SourceType {
    GITHUB,
    ARCHIVE_ORG,
    PLAIN_HTTPS,
    MANIFESTHUB,
    HUBCAP,
    RYUU
}

@Keep
data class DepotSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val url: String,
    val apiKey: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0           // lower = tried first
)

// ─── Settings ────────────────────────────────────────────────────────────

@Keep
data class AppSettings(
    val outputDirectory: String = "",
    val sources: List<DepotSource> = defaultSources(),
    val speedLimitBytesPerSec: Long = 0L,    // 0 = unlimited
    val theme: AppTheme = AppTheme.DARK,
    val language: String = "en",
    val shutdownAfterDownload: Boolean = false,
    val likeSteamMode: Boolean = false,
    val manifesthubApiKey: String = "",
    val hubcapApiKey: String = "",
    val ryuuApiKey: String = "",
    val proxyUrl: String = "",               // e.g. socks5://host:port or http://...
    val telemetryEnabled: Boolean = false
)

enum class AppTheme { DARK, LIGHT, SYSTEM }

fun defaultSources() = listOf(
    DepotSource(
        id = "manifesthub_archive",
        name = "ManifestHub (archive.org)",
        type = SourceType.ARCHIVE_ORG,
        url = "https://archive.org/download/manifest-hub-repo",
        priority = 0
    ),
    DepotSource(
        id = "github_steamautocracks",
        name = "GitHub – SteamAutoCracks",
        type = SourceType.GITHUB,
        url = "https://github.com/SteamAutoCracks/ManifestHub",
        priority = 1
    ),
    DepotSource(
        id = "manifesthub_api",
        name = "ManifestHub API",
        type = SourceType.MANIFESTHUB,
        url = "https://manifesthub2.filegear-sg.me",
        priority = 2
    ),
    DepotSource(
        id = "hubcap",
        name = "Hubcap",
        type = SourceType.HUBCAP,
        url = "https://hubcapmanifest.com",
        priority = 3
    ),
    DepotSource(
        id = "ryuu",
        name = "Ryuu",
        type = SourceType.RYUU,
        url = "https://generator.ryuu.lol",
        priority = 4
    )
)
