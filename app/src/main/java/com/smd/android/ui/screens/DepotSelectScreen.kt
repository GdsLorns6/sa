package com.smd.android.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smd.android.model.DepotEntry
import com.smd.android.ui.theme.*
import com.smd.android.util.FormatUtil
import com.smd.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepotSelectScreen(
    viewModel: MainViewModel,
    onNavigateToDownload: () -> Unit,
    onNavigateBack: () -> Unit,
    onAddToQueue: () -> Unit
) {
    val manifest by viewModel.parsedManifest.collectAsState()
    val gameArtUrl by viewModel.gameArtUrl.collectAsState()
    var filterQuery by remember { mutableStateOf("") }

    val m = manifest ?: return

    val filteredDepots = if (filterQuery.isBlank()) m.depots
    else m.depots.filter { d ->
        d.depotId.toString().contains(filterQuery) ||
                d.name?.contains(filterQuery, ignoreCase = true) == true ||
                d.osArch?.contains(filterQuery, ignoreCase = true) == true ||
                d.language?.contains(filterQuery, ignoreCase = true) == true
    }

    val selectedCount = m.depots.count { it.isSelected }
    val totalSizeSelected = m.depots.filter { it.isSelected }.sumOf { it.sizeBytes ?: 0L }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Depots", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SteamSurface)
            )
        },
        bottomBar = {
            DepotSelectBottomBar(
                selectedCount = selectedCount,
                totalSizeSelected = totalSizeSelected,
                onDownload = onNavigateToDownload,
                onAddToQueue = onAddToQueue,
                onSelectAll = { viewModel.selectAllDepots(true) },
                onSelectNone = { viewModel.selectAllDepots(false) },
                onLikeSteam = { viewModel.autoSelectDepots() }
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Game info header ─────────────────────────────────────
            item {
                GameInfoHeader(
                    appId = m.appId,
                    appName = m.appName ?: "App ${m.appId}",
                    artUrl = gameArtUrl,
                    totalDepots = m.depots.size,
                    sourceName = m.sourceName
                )
            }

            // ─── Filter bar ───────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    label = { Text("Filter depots", color = SteamGray) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = SteamGray) },
                    trailingIcon = {
                        if (filterQuery.isNotBlank()) {
                            IconButton(onClick = { filterQuery = "" }) {
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

            // ─── Depot list ───────────────────────────────────────────
            items(filteredDepots, key = { it.depotId }) { depot ->
                DepotCard(
                    depot = depot,
                    onToggle = { viewModel.toggleDepot(depot.depotId) }
                )
            }
        }
    }
}

@Composable
private fun GameInfoHeader(
    appId: Long,
    appName: String,
    artUrl: String?,
    totalDepots: Int,
    sourceName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SteamSurfaceVariant),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (artUrl != null) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = appName,
                    modifier = Modifier
                        .size(120.dp, 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appName,
                    color = SteamOnSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("AppID: $appId", color = SteamGray, style = MaterialTheme.typography.bodySmall)
                Text("Depots: $totalDepots", color = SteamGray, style = MaterialTheme.typography.bodySmall)
                if (sourceName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, null, tint = SteamAccent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(sourceName, color = SteamAccent, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DepotCard(depot: DepotEntry, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (depot.isSelected) SteamAccent.copy(alpha = 0.12f) else SteamSurface
        ),
        border = BorderStroke(
            1.dp,
            if (depot.isSelected) SteamAccent else SteamDivider
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = depot.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SteamAccent,
                    uncheckedColor = SteamGray
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Depot ${depot.depotId}",
                        color = SteamOnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (depot.name != null) {
                        Text(
                            " — ${depot.name}",
                            color = SteamGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    depot.osArch?.let { DepotTag(it, SteamBlueLight, SteamAccent) }
                    depot.language?.let { DepotTag(it, SteamSurfaceVariant, SteamGray) }
                    depot.sizeBytes?.let {
                        if (it > 0) DepotTag(FormatUtil.formatBytes(it), SteamSurfaceVariant, SteamGray)
                    }
                }
                if (depot.manifestId != null) {
                    Text(
                        "Manifest: ${depot.manifestId}",
                        color = SteamGray.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DepotTag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DepotSelectBottomBar(
    selectedCount: Int,
    totalSizeSelected: Long,
    onDownload: () -> Unit,
    onAddToQueue: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onLikeSteam: () -> Unit
) {
    Surface(color = SteamSurface, tonalElevation = 8.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Quick-select row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SteamDivider)
                ) { Text("All", color = SteamGray) }
                OutlinedButton(
                    onClick = onSelectNone,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SteamDivider)
                ) { Text("None", color = SteamGray) }
                OutlinedButton(
                    onClick = onLikeSteam,
                    modifier = Modifier.weight(1.5f),
                    border = BorderStroke(1.dp, SteamAccentDim)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = SteamAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Like Steam", color = SteamAccent)
                }
            }
            // Selection info + action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$selectedCount depot(s) selected", color = SteamOnSurface, fontWeight = FontWeight.Medium)
                    if (totalSizeSelected > 0)
                        Text(FormatUtil.formatBytes(totalSizeSelected), color = SteamGray, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onAddToQueue,
                    enabled = selectedCount > 0,
                    border = BorderStroke(1.dp, SteamAccentDim),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.AddToQueue, null, tint = SteamAccentDim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Queue", color = SteamAccentDim)
                }
                Button(
                    onClick = onDownload,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SteamAccent)
                ) {
                    Icon(Icons.Filled.Download, null, tint = SteamBackground, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Download", color = SteamBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
