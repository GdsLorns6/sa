package com.smd.android.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.smd.android.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

private const val TAG = "SteamAPI"

/**
 * Networking layer — OkHttp client factory + all API calls.
 * Mirrors the Rust services: steam_store_api, steam_pics, multi_repo_search,
 * manifest_hub_api, hubcap_api, ryuu_api, depot_sources.
 */
object SteamApiClient {

    private val gson = Gson()
    private var client: OkHttpClient = buildClient()

    fun updateClient(proxyUrl: String = "") {
        client = buildClient(proxyUrl)
    }

    private fun buildClient(proxyUrl: String = ""): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (proxyUrl.isNotBlank()) {
            try {
                val uri = java.net.URI(proxyUrl)
                val proxyType = when {
                    uri.scheme.startsWith("socks") -> Proxy.Type.SOCKS
                    else -> Proxy.Type.HTTP
                }
                val proxy = Proxy(proxyType, InetSocketAddress(uri.host, uri.port))
                builder.proxy(proxy)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid proxy URL: $proxyUrl")
            }
        }

        return builder.build()
    }

    // ─── Steam Store API ───────────────────────────────────────────────────

    suspend fun fetchGameInfo(appId: Long): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://store.steampowered.com/api/appdetails?appids=$appId&filters=basic"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext Pair(null, null)
            val json = JsonParser.parseString(body).asJsonObject
            val data = json.getAsJsonObject(appId.toString())
                ?.getAsJsonObject("data") ?: return@withContext Pair(null, null)
            val name = data.get("name")?.asString
            val coverUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/header.jpg"
            Pair(name, coverUrl)
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameInfo failed", e)
            Pair(null, null)
        }
    }

    // ─── Steam PICS (via api.steamcmd.net) ────────────────────────────────

    suspend fun fetchDepotInfos(appId: Long): List<DepotInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.steamcmd.net/v1/info/$appId"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseDepotInfos(body, appId)
        } catch (e: Exception) {
            Log.e(TAG, "fetchDepotInfos failed", e)
            emptyList()
        }
    }

    private fun parseDepotInfos(json: String, appId: Long): List<DepotInfo> {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val appData = root.getAsJsonObject("data")
                ?.getAsJsonObject(appId.toString())
                ?.getAsJsonObject("depots") ?: return emptyList()

            val result = mutableListOf<DepotInfo>()
            appData.entrySet().forEach { (depotId, depotVal) ->
                val depotId2 = depotId.toLongOrNull() ?: return@forEach
                val obj = depotVal.asJsonObject ?: return@forEach
                val name = obj.get("name")?.asString
                val maxSize = obj.get("maxsize")?.asLong
                val config = obj.getAsJsonObject("config")?.let { cfg ->
                    DepotConfig(
                        oslist = cfg.get("oslist")?.asString,
                        osarch = cfg.get("osarch")?.asString,
                        language = cfg.get("language")?.asString
                    )
                }
                result += DepotInfo(depotId2, name, maxSize, config)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── App search (Steam Store search) ──────────────────────────────────

    suspend fun searchGames(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://store.steampowered.com/api/storesearch/?term=$encodedQuery&l=english&cc=US"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseSearchResults(body)
        } catch (e: Exception) {
            Log.e(TAG, "searchGames failed", e)
            emptyList()
        }
    }

    private fun parseSearchResults(json: String): List<SearchResult> {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val items = root.getAsJsonArray("items") ?: return emptyList()
            items.mapNotNull { el ->
                val obj = el.asJsonObject
                val id = obj.get("id")?.asLong ?: return@mapNotNull null
                val name = obj.get("name")?.asString ?: return@mapNotNull null
                val tiny = obj.getAsJsonObject("tiny_image")?.get("url")?.asString
                SearchResult(appId = id, appName = name, coverUrl = tiny)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── Depot sources: find manifest + key by AppID ──────────────────────

    /**
     * Queries the configured sources in priority order and returns the first
     * ParsedManifest found. Mirrors smd-core's multi_repo_search / depot_sources logic.
     */
    suspend fun findManifestByAppId(
        appId: Long,
        sources: List<DepotSource>,
        manifesthubKey: String = "",
        hubcapKey: String = "",
        ryuuKey: String = "",
        targetSourceId: String? = null
    ): ParsedManifest? = withContext(Dispatchers.IO) {
        val eligibleSources = if (!targetSourceId.isNullOrBlank()) {
            sources.filter { it.id == targetSourceId }
        } else {
            sources.filter { it.isEnabled }.sortedBy { it.priority }
        }

        for (source in eligibleSources) {
            val keyForSource = source.apiKey?.takeIf { it.isNotBlank() }
            val result = when (source.type) {
                SourceType.ARCHIVE_ORG -> fetchFromArchiveOrg(appId)
                SourceType.GITHUB -> fetchFromGitHub(appId, source.url)
                SourceType.MANIFESTHUB -> {
                    val k = keyForSource ?: manifesthubKey
                    if (k.isNotBlank()) fetchFromManifestHub(appId, k) else null
                }
                SourceType.HUBCAP -> {
                    val k = keyForSource ?: hubcapKey
                    if (k.isNotBlank()) fetchFromHubcap(appId, k) else null
                }
                SourceType.RYUU -> {
                    val k = keyForSource ?: ryuuKey
                    if (k.isNotBlank()) fetchFromRyuu(appId, k) else null
                }
                SourceType.PLAIN_HTTPS -> fetchFromPlainHttps(appId, source.url)
            }
            if (result != null && result.depots.isNotEmpty()) {
                return@withContext result.copy(sourceName = source.name)
            }
        }
        null
    }

    // ─── archive.org / ManifestHub archive ────────────────────────────────

    private fun fetchFromArchiveOrg(appId: Long): ParsedManifest? {
        return try {
            // The ManifestHub archive stores .lua files as AppID.lua
            val url = "https://archive.org/download/manifest-hub-repo/$appId.lua"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val content = resp.body?.string() ?: return null
            LuaParser.parse(content)
        } catch (e: Exception) {
            null
        }
    }

    // ─── GitHub manifest repos ────────────────────────────────────────────

    private fun fetchFromGitHub(appId: Long, repoUrl: String): ParsedManifest? {
        return try {
            // Convert "https://github.com/Owner/Repo" → raw URL
            val parts = repoUrl.trimEnd('/').split("/")
            val owner = parts.getOrNull(parts.size - 2) ?: return null
            val repo = parts.lastOrNull() ?: return null
            val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/main/$appId.lua"
            val req = Request.Builder().url(rawUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                // Try .st extension fallback
                val rawUrlSt = "https://raw.githubusercontent.com/$owner/$repo/main/$appId.st"
                val req2 = Request.Builder().url(rawUrlSt).build()
                val resp2 = client.newCall(req2).execute()
                if (!resp2.isSuccessful) return null
                return StParser.parse(resp2.body?.string() ?: return null)
            }
            LuaParser.parse(resp.body?.string() ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    // ─── ManifestHub API ──────────────────────────────────────────────────

    private fun fetchFromManifestHub(appId: Long, apiKey: String): ParsedManifest? {
        return try {
            val url = "https://manifesthub2.filegear-sg.me/api/app/$appId"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            parseManifestHubResponse(body, appId)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseManifestHubResponse(json: String, appId: Long): ParsedManifest? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val depotsArr = root.getAsJsonArray("depots") ?: return null
            val depots = depotsArr.mapNotNull { el ->
                val obj = el.asJsonObject
                val depotId = obj.get("depot_id")?.asLong ?: return@mapNotNull null
                val manifestId = obj.get("manifest_id")?.asLong
                val key = obj.get("depot_key")?.asString
                DepotEntry(depotId = depotId, manifestId = manifestId, decryptionKey = key)
            }
            ParsedManifest(appId = appId, appName = null, depots = depots)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Hubcap ───────────────────────────────────────────────────────────

    private fun fetchFromHubcap(appId: Long, apiKey: String): ParsedManifest? {
        return try {
            val url = "https://hubcapmanifest.com/api/app/$appId"
            val req = Request.Builder()
                .url(url)
                .header("X-API-Key", apiKey)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            parseGenericManifestJson(body, appId)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Ryuu ─────────────────────────────────────────────────────────────

    private fun fetchFromRyuu(appId: Long, apiKey: String): ParsedManifest? {
        return try {
            val url = "https://generator.ryuu.lol/api/generate?appid=$appId"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", apiKey)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            // Ryuu typically returns .lua content directly
            LuaParser.parse(body) ?: parseGenericManifestJson(body, appId)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Plain HTTPS folder ───────────────────────────────────────────────

    private fun fetchFromPlainHttps(appId: Long, baseUrl: String): ParsedManifest? {
        return try {
            val url = "${baseUrl.trimEnd('/')}/$appId.lua"
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            LuaParser.parse(resp.body?.string() ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Generic JSON depot format helper ────────────────────────────────

    private fun parseGenericManifestJson(json: String, appId: Long): ParsedManifest? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val depotsArr = root.getAsJsonArray("depots")
                ?: root.getAsJsonArray("data")
                ?: return null
            val depots = depotsArr.mapNotNull { el ->
                val obj = el.asJsonObject
                val depotId = (obj.get("depot_id") ?: obj.get("depotId") ?: obj.get("id"))
                    ?.asLong ?: return@mapNotNull null
                val manifestId = (obj.get("manifest_id") ?: obj.get("manifestId"))?.asLong
                val key = (obj.get("depot_key") ?: obj.get("decryptionKey") ?: obj.get("key"))?.asString
                DepotEntry(depotId = depotId, manifestId = manifestId, decryptionKey = key)
            }
            ParsedManifest(appId = appId, appName = null, depots = depots)
        } catch (e: Exception) {
            null
        }
    }

    // ─── Proxy test ───────────────────────────────────────────────────────

    suspend fun testProxy(proxyUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val testClient = buildClient(proxyUrl)
            val req = Request.Builder().url("https://store.steampowered.com/api/appdetails?appids=730&filters=basic").build()
            val resp = testClient.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
