package com.smd.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.smd.android.model.AppSettings
import com.smd.android.model.AppTheme
import com.smd.android.network.SteamApiClient
import com.smd.android.ui.theme.*
import com.smd.android.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToServerManager: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var localSettings by remember(settings) { mutableStateOf(settings) }

    var proxyTestResult by remember { mutableStateOf<Boolean?>(null) }
    var isTestingProxy by remember { mutableStateOf(false) }
    var showApiKeyVisibility by remember { mutableStateOf(mapOf<String, Boolean>()) }

    fun toggleVisibility(key: String) {
        showApiKeyVisibility = showApiKeyVisibility.toMutableMap().also { it[key] = !(it[key] ?: false) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = SteamAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveSettings(localSettings)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = SteamAccent)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveSettings(localSettings)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Download Settings ─────────────────────────────────────
            item {
                SettingsSectionHeader("📥 Download")
            }

            item {
                OutlinedTextField(
                    value = localSettings.outputDirectory,
                    onValueChange = { localSettings = localSettings.copy(outputDirectory = it) },
                    label = { Text("Output Directory", color = SteamGray) },
                    placeholder = { Text("Leave blank for default (app storage)", color = SteamGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Folder, null, tint = SteamGray) },
                    colors = smdTextFieldColors()
                )
            }

            // Speed limit
            item {
                var speedText by remember {
                    mutableStateOf(
                        if (localSettings.speedLimitBytesPerSec > 0)
                            (localSettings.speedLimitBytesPerSec / 1024 / 1024).toString()
                        else ""
                    )
                }
                OutlinedTextField(
                    value = speedText,
                    onValueChange = {
                        speedText = it.filter { c -> c.isDigit() }
                        localSettings = localSettings.copy(
                            speedLimitBytesPerSec = speedText.toLongOrNull()?.times(1024 * 1024) ?: 0L
                        )
                    },
                    label = { Text("Speed Limit (MB/s, 0 = unlimited)", color = SteamGray) },
                    placeholder = { Text("e.g. 10", color = SteamGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Filled.Speed, null, tint = SteamGray) },
                    colors = smdTextFieldColors()
                )
            }

            // Like Steam mode
            item {
                SettingsToggle(
                    title = "Like Steam Mode",
                    subtitle = "Auto-select base game depots for your platform/language",
                    checked = localSettings.likeSteamMode,
                    onCheckedChange = { localSettings = localSettings.copy(likeSteamMode = it) },
                    icon = Icons.Filled.AutoAwesome
                )
            }

            // Shutdown after download
            item {
                SettingsToggle(
                    title = "Shut Down When Finished",
                    subtitle = "Power off device when all downloads complete",
                    checked = localSettings.shutdownAfterDownload,
                    onCheckedChange = { localSettings = localSettings.copy(shutdownAfterDownload = it) },
                    icon = Icons.Filled.PowerSettingsNew
                )
            }

            item { Divider(color = SteamDivider) }

            // ─── Appearance ────────────────────────────────────────────
            item {
                SettingsSectionHeader("🎨 Appearance")
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, null, tint = SteamGray)
                    Spacer(Modifier.width(12.dp))
                    Text("Theme", color = SteamOnSurface, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = localSettings.theme == theme,
                                onClick = { localSettings = localSettings.copy(theme = theme) },
                                label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SteamAccent,
                                    selectedLabelColor = SteamBackground
                                )
                            )
                        }
                    }
                }
            }

            item { Divider(color = SteamDivider) }

            // ─── Sources / API Keys ────────────────────────────────────
            item { SettingsSectionHeader("🌐 Sources & API Keys") }

            item {
                Button(
                    onClick = onNavigateToServerManager,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SteamSurfaceVariant),
                    border = BorderStroke(1.dp, SteamDivider)
                ) {
                    Icon(Icons.Filled.Storage, null, tint = SteamAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage Download Sources", color = SteamOnSurface)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = SteamGray)
                }
            }

            // ManifestHub API Key
            item {
                ApiKeyField(
                    label = "ManifestHub API Key",
                    value = localSettings.manifesthubApiKey,
                    onChange = { localSettings = localSettings.copy(manifesthubApiKey = it) },
                    visible = showApiKeyVisibility["mh"] ?: false,
                    onToggleVisibility = { toggleVisibility("mh") }
                )
            }

            // Hubcap API Key
            item {
                ApiKeyField(
                    label = "Hubcap API Key",
                    value = localSettings.hubcapApiKey,
                    onChange = { localSettings = localSettings.copy(hubcapApiKey = it) },
                    visible = showApiKeyVisibility["hc"] ?: false,
                    onToggleVisibility = { toggleVisibility("hc") }
                )
            }

            // Ryuu API Key
            item {
                ApiKeyField(
                    label = "Ryuu API Key",
                    value = localSettings.ryuuApiKey,
                    onChange = { localSettings = localSettings.copy(ryuuApiKey = it) },
                    visible = showApiKeyVisibility["ryuu"] ?: false,
                    onToggleVisibility = { toggleVisibility("ryuu") }
                )
            }

            item { Divider(color = SteamDivider) }

            // ─── Network / Proxy ───────────────────────────────────────
            item { SettingsSectionHeader("🔒 Network & Proxy") }

            item {
                OutlinedTextField(
                    value = localSettings.proxyUrl,
                    onValueChange = { localSettings = localSettings.copy(proxyUrl = it) },
                    label = { Text("Proxy URL", color = SteamGray) },
                    placeholder = { Text("http://host:port or socks5://...", color = SteamGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.VpnKey, null, tint = SteamGray) },
                    colors = smdTextFieldColors()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            isTestingProxy = true
                            scope.launch {
                                proxyTestResult = SteamApiClient.testProxy(localSettings.proxyUrl)
                                isTestingProxy = false
                            }
                        },
                        enabled = localSettings.proxyUrl.isNotBlank() && !isTestingProxy,
                        border = BorderStroke(1.dp, SteamAccentDim)
                    ) {
                        if (isTestingProxy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SteamAccent)
                        } else {
                            Icon(Icons.Filled.NetworkCheck, null, tint = SteamAccent, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Test Proxy", color = SteamAccent)
                    }
                    proxyTestResult?.let { ok ->
                        Text(
                            if (ok) "✅ Connection OK" else "❌ Connection failed",
                            color = if (ok) SteamGreen else SteamRed,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            item { Divider(color = SteamDivider) }

            // ─── Privacy ───────────────────────────────────────────────
            item { SettingsSectionHeader("🔏 Privacy") }

            item {
                SettingsToggle(
                    title = "Anonymous Telemetry",
                    subtitle = "Send anonymous crash and usage statistics",
                    checked = localSettings.telemetryEnabled,
                    onCheckedChange = { localSettings = localSettings.copy(telemetryEnabled = it) },
                    icon = Icons.Filled.Analytics
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = SteamAccent,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SteamGray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SteamOnSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SteamGray, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SteamBackground,
                checkedTrackColor = SteamAccent,
                uncheckedThumbColor = SteamGray,
                uncheckedTrackColor = SteamDivider
            )
        )
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = SteamGray) },
        placeholder = { Text("Enter API key", color = SteamGray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null,
                    tint = SteamGray
                )
            }
        },
        leadingIcon = { Icon(Icons.Filled.Key, null, tint = SteamGray) },
        colors = smdTextFieldColors()
    )
}

@Composable
private fun smdTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SteamAccent,
    unfocusedBorderColor = SteamDivider,
    focusedTextColor = SteamOnSurface,
    unfocusedTextColor = SteamOnSurface,
    cursorColor = SteamAccent
)
