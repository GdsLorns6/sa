package com.smd.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smd.android.model.QueueEntry
import com.smd.android.ui.theme.*
import com.smd.android.util.FormatUtil
import com.smd.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val queue by viewModel.queue.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Queue", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                actions = {
                    if (queue.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearQueue() }) {
                            Text("Clear All", color = SteamRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SteamSurface)
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        if (queue.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QueueMusic, null, tint = SteamGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("The download queue is empty", color = SteamGray)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select depots and tap 'Add to Queue' to line up multiple downloads",
                        color = SteamGray.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
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
                item {
                    Text(
                        "${queue.size} download(s) in queue",
                        color = SteamGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(queue, key = { it.id }) { entry ->
                    QueueCard(
                        entry = entry,
                        onRemove = { viewModel.removeFromQueue(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueCard(entry: QueueEntry, onRemove: () -> Unit) {
    val manifest = entry.manifest
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SteamSurface),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HourglassEmpty, null, tint = SteamAccent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    manifest.appName ?: "App ${manifest.appId}",
                    color = SteamOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "AppID: ${manifest.appId} · ${entry.selectedDepots.size} depots",
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Added: ${FormatUtil.formatTimestamp(entry.addedAt)}",
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.RemoveCircle, "Remove", tint = SteamRed)
            }
        }
    }
}
