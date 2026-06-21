package com.sshvan.tunnelmanager.presentation.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedProfiles by remember { mutableStateOf(setOf<Long>()) }
    var exportAsLocked by remember { mutableStateOf(false) }


    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SettingsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            viewModel.exportProfiles(it, selectedProfiles, exportAsLocked) 
            showExportDialog = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importProfiles(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2C) else Color(0xFF313033),
                    contentColor = Color.White,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingItem(
                title = "Export Profiles",
                description = "Backup all profiles to a JSON file",
                icon = { Icon(Icons.Default.Upload, contentDescription = null) },
                onClick = { 
                    selectedProfiles = profiles.map { it.id }.toSet()
                    showExportDialog = true 
                }
            )
            Divider()
            SettingItem(
                title = "Import Profiles",
                description = "Restore profiles from a JSON file",
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )
            Divider()
        }
        
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Profiles") },
                text = {
                    Column {
                        Text("Select profiles to export:")
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(profiles) { profile ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedProfiles.contains(profile.id)) {
                                                selectedProfiles = selectedProfiles - profile.id
                                            } else {
                                                selectedProfiles = selectedProfiles + profile.id
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedProfiles.contains(profile.id),
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(profile.name)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exportAsLocked = !exportAsLocked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = exportAsLocked,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Export as Locked Profiles")
                                Text("Others won't be able to edit or see the settings of these profiles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { exportLauncher.launch("sshvan_profiles.json") },
                        enabled = selectedProfiles.isNotEmpty()
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingItemWithSwitch(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
