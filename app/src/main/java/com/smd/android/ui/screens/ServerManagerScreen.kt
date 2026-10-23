package com.smd.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smd.android.model.DepotSource
import com.smd.android.model.SourceType
import com.smd.android.ui.theme.*
import com.smd.android.viewmodel.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var sources by remember(settings.sources) { mutableStateOf(settings.sources) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<DepotSource?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Servers & Sources", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveSettings(settings.copy(sources = sources))
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Server", tint = SteamAccent)
                    }
                    TextButton(onClick = {
                        viewModel.saveSettings(settings.copy(sources = sources))
                        onNavigateBack()
                    }) {
                        Text("Save", color = SteamAccent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SteamSurface)
            )
        },
        containerColor = SteamBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SteamBlueLight.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, SteamAccentDim)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, tint = SteamAccent)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Choose which servers/sources to query and set their priority order. Tap arrows to reorder, switch toggles to enable/disable, or add your custom server.",
                            color = SteamOnSurface,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            itemsIndexed(sources, key = { _, s -> s.id }) { index, source ->
                ServerItemCard(
                    source = source,
                    isFirst = index == 0,
                    isLast = index == sources.size - 1,
                    onToggleEnabled = { enabled ->
                        sources = sources.toMutableList().also { list ->
                            list[index] = list[index].copy(isEnabled = enabled)
                        }
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val list = sources.toMutableList()
                            val temp = list[index]
                            list[index] = list[index - 1].copy(priority = index)
                            list[index - 1] = temp.copy(priority = index - 1)
                            sources = list
                        }
                    },
                    onMoveDown = {
                        if (index < sources.size - 1) {
                            val list = sources.toMutableList()
                            val temp = list[index]
                            list[index] = list[index + 1].copy(priority = index)
                            list[index + 1] = temp.copy(priority = index + 1)
                            sources = list
                        }
                    },
                    onEdit = { editingSource = source },
                    onDelete = {
                        sources = sources.filter { it.id != source.id }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditServerDialog(
            initialSource = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newSource ->
                sources = sources + newSource.copy(priority = sources.size)
                showAddDialog = false
            }
        )
    }

    editingSource?.let { src ->
        AddEditServerDialog(
            initialSource = src,
            onDismiss = { editingSource = null },
            onConfirm = { updated ->
                sources = sources.map { if (it.id == updated.id) updated else it }
                editingSource = null
            }
        )
    }
}

@Composable
private fun ServerItemCard(
    source: DepotSource,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (source.isEnabled) SteamSurfaceVariant else SteamSurface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (source.isEnabled) SteamDivider else SteamDivider.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        "Move Up",
                        tint = if (!isFirst) SteamAccent else SteamGray.copy(alpha = 0.3f)
                    )
                }
                Text(
                    "#${source.priority + 1}",
                    color = SteamAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        "Move Down",
                        tint = if (!isLast) SteamAccent else SteamGray.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        source.name,
                        color = if (source.isEnabled) SteamOnSurface else SteamGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(SteamBlueLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            source.type.name.replace("_", " "),
                            color = SteamAccent,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    source.url,
                    color = SteamGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                if (!source.apiKey.isNullOrBlank()) {
                    Text(
                        "API Key configured",
                        color = SteamGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Switch(
                checked = source.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SteamBackground,
                    checkedTrackColor = SteamAccent,
                    uncheckedThumbColor = SteamGray,
                    uncheckedTrackColor = SteamDivider
                )
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = SteamRed.copy(alpha = 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditServerDialog(
    initialSource: DepotSource?,
    onDismiss: () -> Unit,
    onConfirm: (DepotSource) -> Unit
) {
    var name by remember { mutableStateOf(initialSource?.name ?: "") }
    var url by remember { mutableStateOf(initialSource?.url ?: "") }
    var apiKey by remember { mutableStateOf(initialSource?.apiKey ?: "") }
    var selectedType by remember { mutableStateOf(initialSource?.type ?: SourceType.GITHUB) }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSource == null) "Add Download Server" else "Edit Server", color = SteamAccent) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Server Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        SourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server Base URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    placeholder = { Text("Token or key if required") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSource = initialSource?.copy(
                        name = name,
                        type = selectedType,
                        url = url,
                        apiKey = apiKey.ifBlank { null }
                    ) ?: DepotSource(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = selectedType,
                        url = url,
                        apiKey = apiKey.ifBlank { null }
                    )
                    onConfirm(finalSource)
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SteamAccent)
            ) {
                Text("Save", color = SteamBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = SteamSurfaceVariant
    )
}
