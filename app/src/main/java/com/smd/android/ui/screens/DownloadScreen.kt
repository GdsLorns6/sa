package com.smd.android.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smd.android.model.DownloadStatus
import com.smd.android.ui.theme.*
import com.smd.android.util.FormatUtil
import com.smd.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val manifest by viewModel.parsedManifest.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()
    val logLines by viewModel.downloadLog.collectAsState()
    val isRunning by viewModel.isDownloadRunning.collectAsState()

    val logListState = rememberLazyListState()
    var hasStarted by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf("") }

    // Notification permission (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result */ }

    // Auto-scroll log to bottom
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            logListState.animateScrollToItem(logLines.size - 1)
        }
    }

    // Start the download on first composition
    LaunchedEffect(Unit) {
        if (!hasStarted) {
            hasStarted = true
            // Request notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.startDownload(outputPath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloading", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isRunning || progress.status == DownloadStatus.COMPLETED
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SteamSurface)
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── Game info ─────────────────────────────────────────────
            manifest?.let { m ->
                Text(
                    m.appName ?: "App ${m.appId}",
                    style = MaterialTheme.typography.titleLarge,
                    color = SteamOnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text("AppID: ${m.appId}", color = SteamGray)
            }

            // ─── Progress card ─────────────────────────────────────────
            ProgressCard(
                status = progress.status,
                completedDepots = progress.completedDepots,
                totalDepots = progress.totalDepots,
                bytesDownloaded = progress.bytesDownloaded,
                totalBytes = progress.totalBytes,
                speedBytesPerSec = progress.speedBytesPerSec,
                etaSeconds = progress.etaSeconds
            )

            // ─── Control buttons ───────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    progress.status == DownloadStatus.PAUSED -> {
                        Button(
                            onClick = { viewModel.resumeDownload() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SteamGreen)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Resume")
                        }
                    }
                    isRunning -> {
                        Button(
                            onClick = { viewModel.pauseDownload() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SteamOrange)
                        ) {
                            Icon(Icons.Filled.Pause, null, tint = SteamBackground)
                            Spacer(Modifier.width(4.dp))
                            Text("Pause", color = SteamBackground)
                        }
                    }
                    progress.status == DownloadStatus.COMPLETED -> {
                        Button(
                            onClick = onDone,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SteamGreen)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }

                if (isRunning) {
                    OutlinedButton(
                        onClick = { viewModel.cancelDownload() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, SteamRed)
                    ) {
                        Icon(Icons.Filled.Cancel, null, tint = SteamRed)
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel", color = SteamRed)
                    }
                }
            }

            // ─── Download log ──────────────────────────────────────────
            Text(
                "Download Log",
                style = MaterialTheme.typography.titleSmall,
                color = SteamAccent,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
                border = BorderStroke(1.dp, SteamDivider)
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    reverseLayout = false
                ) {
                    items(logLines) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = when {
                                line.startsWith("❌") || line.startsWith("✖") -> SteamRed
                                line.startsWith("✅") || line.startsWith("🎉") -> SteamGreen
                                line.startsWith("⚠") -> SteamOrange
                                line.startsWith("📋") || line.startsWith("📄") -> SteamAccent
                                line.startsWith("─") -> SteamDivider
                                else -> SteamOnSurface
                            },
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    status: DownloadStatus,
    completedDepots: Int,
    totalDepots: Int,
    bytesDownloaded: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    etaSeconds: Long
) {
    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat()
    else if (totalDepots > 0) completedDepots.toFloat() / totalDepots.toFloat()
    else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "progress_anim")
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "progress"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SteamSurfaceVariant),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Status badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status)
                Spacer(Modifier.weight(1f))
                Text(
                    "$completedDepots/$totalDepots depots",
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (status) {
                    DownloadStatus.COMPLETED -> SteamGreen
                    DownloadStatus.FAILED -> SteamRed
                    DownloadStatus.PAUSED -> SteamOrange
                    else -> SteamAccent
                },
                trackColor = SteamDivider
            )

            // Stats row
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "Downloaded",
                    value = if (totalBytes > 0)
                        "${FormatUtil.formatBytes(bytesDownloaded)} / ${FormatUtil.formatBytes(totalBytes)}"
                    else FormatUtil.formatBytes(bytesDownloaded),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Speed",
                    value = FormatUtil.formatSpeed(speedBytesPerSec),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "ETA",
                    value = FormatUtil.formatEta(etaSeconds),
                    modifier = Modifier.weight(1f)
                )
            }

            // Percent
            if (progress > 0) {
                Text(
                    "${(progress * 100).toInt()}%",
                    color = SteamAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.DOWNLOADING -> "Downloading" to SteamAccent
        DownloadStatus.PAUSED -> "Paused" to SteamOrange
        DownloadStatus.COMPLETED -> "Complete" to SteamGreen
        DownloadStatus.FAILED -> "Failed" to SteamRed
        DownloadStatus.CANCELLED -> "Cancelled" to SteamRed
        DownloadStatus.QUEUED -> "Queued" to SteamGray
        else -> "Idle" to SteamGray
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = SteamGray, style = MaterialTheme.typography.bodySmall)
        Text(value, color = SteamOnSurface, fontWeight = FontWeight.Medium)
    }
}
