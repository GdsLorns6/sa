package com.smd.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.smd.android.model.SearchResult
import com.smd.android.ui.theme.*
import com.smd.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: MainViewModel,
    onNavigateToDepotSelect: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val parsedManifest by viewModel.parsedManifest.collectAsState()
    val parseError by viewModel.parseError.collectAsState()
    val isLoading by viewModel.isLoadingManifest.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedSourceId by viewModel.selectedSourceId.collectAsState()
    var expandedServerDropdown by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var appIdInput by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseFile(it) }
    }

    // Navigate when manifest is parsed
    LaunchedEffect(parsedManifest) {
        if (parsedManifest != null) {
            onNavigateToDepotSelect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = SteamAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Steam Manifest Downloader",
                            fontWeight = FontWeight.Bold,
                            color = SteamAccent
                        )
                    }
                },
                actions = {
                    if (queue.isNotEmpty()) {
                        BadgedBox(badge = { Badge { Text("${queue.size}") } }) {
                            IconButton(onClick = onNavigateToQueue) {
                                Icon(Icons.Filled.List, "Queue")
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, "History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SteamSurface
                )
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Header / Hero ─────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SteamBlueLight, SteamSurface)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.VideogameAsset,
                            contentDescription = null,
                            tint = SteamAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Steam Depot Downloader",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SteamAccent
                        )
                        Text(
                            "Upload a .lua/.st manifest or search by App ID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SteamGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ─── Error message ─────────────────────────────────────────
            if (parseError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SteamRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SteamRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = SteamRed)
                            Spacer(Modifier.width(8.dp))
                            Text(parseError!!, color = SteamRed)
                        }
                    }
                }
            }

            // ─── Loading indicator ─────────────────────────────────────
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SteamAccent)
                            Spacer(Modifier.height(8.dp))
                            Text("Fetching depot information…", color = SteamGray)
                        }
                    }
                }
            }

            // ─── Step 1: Upload File ───────────────────────────────────
            item {
                StepCard(number = "1", title = "Upload Manifest File") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Drop a .lua or .st file containing the depot manifest",
                            color = SteamGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { fileLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SteamAccent)
                        ) {
                            Icon(Icons.Filled.UploadFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Browse .lua / .st File", color = SteamBackground)
                        }
                    }
                }
            }

            // ─── Divider ───────────────────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f), color = SteamDivider)
                    Text("  OR  ", color = SteamGray)
                    Divider(modifier = Modifier.weight(1f), color = SteamDivider)
                }
            }

            // ─── Step 2: Search by App ID ──────────────────────────────
            item {
                StepCard(number = "2", title = "Search by App ID / Name") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Server Selector
                        val selectedServerName = remember(selectedSourceId, settings.sources) {
                            if (selectedSourceId == null) {
                                "Auto (Query all enabled sources)"
                            } else {
                                settings.sources.firstOrNull { it.id == selectedSourceId }?.name ?: "Custom Server"
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedServerDropdown,
                            onExpandedChange = { expandedServerDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedServerName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Download Server / Source", color = SteamGray) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Storage, null, tint = SteamAccent)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServerDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SteamAccent,
                                    unfocusedBorderColor = SteamDivider,
                                    focusedTextColor = SteamOnSurface,
                                    unfocusedTextColor = SteamOnSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedServerDropdown,
                                onDismissRequest = { expandedServerDropdown = false },
                                modifier = Modifier.background(SteamSurfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Auto (All Enabled Sources)", fontWeight = FontWeight.Bold, color = SteamAccent)
                                            Text("Tries sources in priority order", fontSize = 11.sp, color = SteamGray)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSelectedSourceId(null)
                                        expandedServerDropdown = false
                                    }
                                )
                                Divider(color = SteamDivider)
                                settings.sources.forEach { source ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(source.name, color = SteamOnSurface, fontWeight = FontWeight.Medium)
                                                    Text(source.type.name.replace("_", " "), fontSize = 11.sp, color = SteamGray)
                                                }
                                                if (source.id == selectedSourceId) {
                                                    Icon(Icons.Filled.Check, null, tint = SteamAccent, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSelectedSourceId(source.id)
                                            expandedServerDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // App ID direct input
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = appIdInput,
                                onValueChange = { appIdInput = it.filter { c -> c.isDigit() } },
                                label = { Text("App ID", color = SteamGray) },
                                placeholder = { Text("e.g. 220", color = SteamGray.copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        appIdInput.toLongOrNull()?.let { viewModel.searchByAppId(it) }
                                        keyboard?.hide()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SteamAccent,
                                    unfocusedBorderColor = SteamDivider,
                                    focusedTextColor = SteamOnSurface,
                                    unfocusedTextColor = SteamOnSurface,
                                    cursorColor = SteamAccent
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    appIdInput.toLongOrNull()?.let { viewModel.searchByAppId(it) }
                                    keyboard?.hide()
                                },
                                enabled = appIdInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = SteamAccent)
                            ) {
                                Icon(Icons.Filled.Search, null, tint = SteamBackground)
                            }
                        }

                        Divider(color = SteamDivider)

                        // Game name search
                        var nameQuery by remember { mutableStateOf("") }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = nameQuery,
                                onValueChange = { nameQuery = it },
                                label = { Text("Game Name", color = SteamGray) },
                                placeholder = { Text("e.g. Half-Life 2", color = SteamGray.copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.searchGames(nameQuery)
                                        keyboard?.hide()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SteamAccent,
                                    unfocusedBorderColor = SteamDivider,
                                    focusedTextColor = SteamOnSurface,
                                    unfocusedTextColor = SteamOnSurface,
                                    cursorColor = SteamAccent
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    viewModel.searchGames(nameQuery)
                                    keyboard?.hide()
                                },
                                enabled = nameQuery.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = SteamAccentDim)
                            ) {
                                Icon(Icons.Filled.Search, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // ─── Search results ────────────────────────────────────────
            if (isSearching) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SteamAccent, modifier = Modifier.size(32.dp))
                    }
                }
            }

            if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        "Search Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = SteamAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(searchResults) { result ->
                    SearchResultCard(result) {
                        viewModel.searchByAppId(result.appId, selectedSourceId)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SteamSurfaceVariant),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SteamAccent, shape = RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(number, color = SteamBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = SteamOnSurface, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SteamSurface),
        border = BorderStroke(1.dp, SteamDivider)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (result.coverUrl != null) {
                AsyncImage(
                    model = result.coverUrl,
                    contentDescription = result.appName,
                    modifier = Modifier
                        .size(60.dp, 28.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.appName,
                    color = SteamOnSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "AppID: ${result.appId}",
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = SteamAccent)
        }
    }
}
