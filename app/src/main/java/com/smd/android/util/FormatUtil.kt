package com.smd.android.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FormatUtil {

    private val df = DecimalFormat("#.##")

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        val value = bytes / 1024.0.pow(exp.toDouble())
        return "${df.format(value)} ${unit}B"
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 B/s"
        if (bytesPerSec < 1024) return "$bytesPerSec B/s"
        val exp = (log10(bytesPerSec.toDouble()) / log10(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1]
        val value = bytesPerSec / 1024.0.pow(exp.toDouble())
        return "${df.format(value)} ${unit}B/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(epochMs))
    }
}
