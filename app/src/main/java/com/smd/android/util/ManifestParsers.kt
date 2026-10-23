package com.smd.android.util

import android.content.Context
import android.net.Uri
import com.smd.android.model.DepotEntry
import com.smd.android.model.ParsedManifest
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parser for Steam .lua manifest files (Lua key-value format used by SteamAutoCracks).
 *
 * Example format:
 * ```
 * AppID = 220
 * "DepotID" = "221"
 * "ManifestID" = "6694553384010890955"
 * "DecryptionKey" = "abc123..."
 * ```
 */
object LuaParser {

    private val APP_ID_REGEX = Regex("""AppID\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val APP_NAME_REGEX = Regex("""AppName\s*=\s*"?([^"\n]+)"?""", RegexOption.IGNORE_CASE)
    private val DEPOT_BLOCK_REGEX = Regex(
        """"?(\d+)"?\s*\{([^}]*)\}""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val KEY_VALUE_REGEX = Regex(""""?(\w+)"?\s+"?([^"\n]+)"?""")

    fun parse(content: String): ParsedManifest? {
        val appId = APP_ID_REGEX.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val appName = APP_NAME_REGEX.find(content)?.groupValues?.get(1)?.trim()

        val depots = mutableListOf<DepotEntry>()

        DEPOT_BLOCK_REGEX.findAll(content).forEach { match ->
            val depotId = match.groupValues[1].toLongOrNull() ?: return@forEach
            val blockContent = match.groupValues[2]

            val kv = mutableMapOf<String, String>()
            KEY_VALUE_REGEX.findAll(blockContent).forEach { kvm ->
                kv[kvm.groupValues[1].lowercase()] = kvm.groupValues[2].trim()
            }

            val manifestId = kv["manifestid"]?.toLongOrNull()
            val decryptionKey = kv["decryptionkey"]

            depots += DepotEntry(
                depotId = depotId,
                manifestId = manifestId,
                decryptionKey = decryptionKey
            )
        }

        // Fallback: simple "DepotID" / "ManifestID" / "DecryptionKey" flat format
        if (depots.isEmpty()) {
            val depotId = KEY_VALUE_REGEX.findAll(content)
                .firstOrNull { it.groupValues[1].lowercase() == "depotid" }
                ?.groupValues?.get(2)?.toLongOrNull()

            if (depotId != null) {
                val manifestId = KEY_VALUE_REGEX.findAll(content)
                    .firstOrNull { it.groupValues[1].lowercase() == "manifestid" }
                    ?.groupValues?.get(2)?.toLongOrNull()
                val decKey = KEY_VALUE_REGEX.findAll(content)
                    .firstOrNull { it.groupValues[1].lowercase() == "decryptionkey" }
                    ?.groupValues?.get(2)

                depots += DepotEntry(
                    depotId = depotId,
                    manifestId = manifestId,
                    decryptionKey = decKey
                )
            }
        }

        return ParsedManifest(appId = appId, appName = appName, depots = depots)
    }

    fun parseFromUri(context: Context, uri: Uri): ParsedManifest? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return null
            parse(content)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Parser for .st (SteamTools) manifest format.
 *
 * Example JSON-like or tab-separated format:
 * ```
 * 220
 * 221 6694553384010890955 abc123...
 * 222 1234567890123456789 def456...
 * ```
 */
object StParser {

    fun parse(content: String): ParsedManifest? {
        val lines = content.trim().lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // First line is AppID
        val appId = lines[0].trim().toLongOrNull() ?: return null
        val depots = mutableListOf<DepotEntry>()

        for (i in 1 until lines.size) {
            val parts = lines[i].trim().split(Regex("\\s+"))
            val depotId = parts.getOrNull(0)?.toLongOrNull() ?: continue
            val manifestId = parts.getOrNull(1)?.toLongOrNull()
            val decryptionKey = parts.getOrNull(2)

            depots += DepotEntry(
                depotId = depotId,
                manifestId = manifestId,
                decryptionKey = decryptionKey
            )
        }

        return ParsedManifest(appId = appId, appName = null, depots = depots)
    }

    fun parseFromUri(context: Context, uri: Uri): ParsedManifest? {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return null
            parse(content)
        } catch (e: Exception) {
            null
        }
    }
}
