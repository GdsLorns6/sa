package com.smd.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smd.android.model.DownloadStatus
import com.smd.android.model.HistoryEntry
import com.smd.android.ui.theme.*
import com.smd.android.util.FormatUtil
import com.smd.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    var filterStatus by remember { mutableStateOf<DownloadStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = history
        .filter { filterStatus == null || it.status == filterStatus }
        .filter { searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { it.downloadedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SteamSurface)
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, null, tint = SteamGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No download history yet", color = SteamGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search history", color = SteamGray) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = SteamGray) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, null, tint = SteamGray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SteamAccent,
                            unfocusedBorderColor = SteamDivider,
                            focusedTextColor = SteamOnSurface,
                            unfocusedTextColor = SteamOnSurface
                        ),
                        singleLine = true
                    )
                }

                // Status filter chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { filterStatus = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SteamAccent,
                                selectedLabelColor = SteamBackground
                            )
                        )
                        FilterChip(
                            selected = filterStatus == DownloadStatus.COMPLETED,
                            onClick = { filterStatus = DownloadStatus.COMPLETED },
                            label = { Text("Completed") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SteamGreen,
                                selectedLabelColor = SteamBackground
                            )
                        )
                        FilterChip(
                            selected = filterStatus == DownloadStatus.FAILED,
                            onClick = { filterStatus = DownloadStatus.FAILED },
                            label = { Text("Failed") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SteamRed,
                                selectedLabelColor = SteamBackground
                            )
                        )
                    }
                }

                items(filtered, key = { it.id }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onDelete = { viewModel.removeHistory(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove from History?") },
            text = { Text("Remove '${entry.appName}' from history? The downloaded files will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Remove", color = SteamRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = SteamSurfaceVariant
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SteamSurface),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Cover art
            if (entry.coverUrl != null) {
                AsyncImage(
                    model = entry.coverUrl,
                    contentDescription = entry.appName,
                    modifier = Modifier
                        .size(80.dp, 38.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.appName,
                    color = SteamOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "AppID: ${entry.appId} · ${entry.selectedDepots.size} depots",
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    FormatUtil.formatTimestamp(entry.downloadedAt),
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
                if (entry.serverUsed.isNotBlank()) {
                    Text(
                        "via ${entry.serverUsed}",
                        color = SteamAccent.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Progress bar if still downloading
                if (entry.totalBytes > 0) {
                    val p = entry.downloadedBytes.toFloat() / entry.totalBytes
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(4.dp),
                        color = when (entry.status) {
                            DownloadStatus.COMPLETED -> SteamGreen
                            DownloadStatus.FAILED -> SteamRed
                            else -> SteamAccent
                        },
                        trackColor = SteamDivider
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    val (statusLabel, statusColor) = when (entry.status) {
                        DownloadStatus.COMPLETED -> "✅ Complete" to SteamGreen
                        DownloadStatus.FAILED -> "❌ Failed" to SteamRed
                        DownloadStatus.DOWNLOADING -> "📥 Downloading" to SteamAccent
                        DownloadStatus.PAUSED -> "⏸ Paused" to SteamOrange
                        DownloadStatus.CANCELLED -> "✖ Cancelled" to SteamRed
                        else -> "❔ Unknown" to SteamGray
                    }
                    Text(statusLabel, color = statusColor, style = MaterialTheme.typography.bodySmall)
                    if (entry.downloadedBytes > 0) {
                        Text(
                            " · ${FormatUtil.formatBytes(entry.downloadedBytes)}",
                            color = SteamGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, "Delete", tint = SteamGray)
            }
        }
    }
}
