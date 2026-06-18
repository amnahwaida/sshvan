package com.sshvan.tunnelmanager.presentation.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sshvan.tunnelmanager.domain.model.AuthType
import com.sshvan.tunnelmanager.presentation.viewmodel.EditProfileEvent
import com.sshvan.tunnelmanager.presentation.viewmodel.EditProfileViewModel
import com.sshvan.tunnelmanager.presentation.viewmodel.TestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isEditMode = viewModel.isEditMode
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is EditProfileEvent.SaveSuccess -> {
                    onNavigateBack()
                }
                is EditProfileEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Profile" else "New Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::saveProfile,
                        enabled = !formState.isSaving
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // General Section
            SectionCard(title = "General") {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Office Server") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formState.errors.containsKey("name"),
                    supportingText = { formState.errors["name"]?.let { Text(it) } },
                    singleLine = true
                )
            }

            // SSH Server Section
            SectionCard(title = "SSH Server") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = formState.sshHost,
                        onValueChange = viewModel::updateSshHost,
                        label = { Text("Host / IP") },
                        modifier = Modifier.weight(2f),
                        isError = formState.errors.containsKey("sshHost"),
                        supportingText = { formState.errors["sshHost"]?.let { Text(it) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formState.sshPort,
                        onValueChange = viewModel::updateSshPort,
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = formState.errors.containsKey("sshPort"),
                        supportingText = { formState.errors["sshPort"]?.let { Text(it) } },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formState.errors.containsKey("username"),
                    supportingText = { formState.errors["username"]?.let { Text(it) } },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Authentication Method", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = formState.authType == AuthType.PASSWORD,
                        onClick = { viewModel.updateAuthType(AuthType.PASSWORD) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Password")
                    }
                    SegmentedButton(
                        selected = formState.authType == AuthType.PRIVATE_KEY,
                        onClick = { viewModel.updateAuthType(AuthType.PRIVATE_KEY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Private Key")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (formState.authType == AuthType.PASSWORD) {
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = formState.errors.containsKey("password"),
                        supportingText = { formState.errors["password"]?.let { Text(it) } },
                        singleLine = true
                    )
                } else {
                    // Simple text field for path for now.
                    // In a real app, this should use a File Picker intent.
                    OutlinedTextField(
                        value = formState.privateKeyPath ?: "",
                        onValueChange = viewModel::updatePrivateKeyPath,
                        label = { Text("Private Key File Path") },
                        placeholder = { Text("/storage/emulated/0/Download/id_rsa") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { /* TODO: Launch file picker */ }) {
                                Icon(Icons.Filled.FileOpen, contentDescription = "Pick File")
                            }
                        },
                        isError = formState.errors.containsKey("privateKeyPath"),
                        supportingText = { formState.errors["privateKeyPath"]?.let { Text(it) } },
                        singleLine = true
                    )
                }
            }

            // Port Forwarding Section
            SectionCard(title = "Port Forwarding") {
                OutlinedTextField(
                    value = formState.localPort,
                    onValueChange = viewModel::updateLocalPort,
                    label = { Text("Local Port") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = formState.errors.containsKey("localPort"),
                    supportingText = { formState.errors["localPort"]?.let { Text(it) } },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = formState.remoteHost,
                        onValueChange = viewModel::updateRemoteHost,
                        label = { Text("Remote Host") },
                        modifier = Modifier.weight(2f),
                        isError = formState.errors.containsKey("remoteHost"),
                        supportingText = { formState.errors["remoteHost"]?.let { Text(it) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formState.remotePort,
                        onValueChange = viewModel::updateRemotePort,
                        label = { Text("Remote Port") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = formState.errors.containsKey("remotePort"),
                        supportingText = { formState.errors["remotePort"]?.let { Text(it) } },
                        singleLine = true
                    )
                }
            }

            // Action Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !formState.isTesting && !formState.isSaving
                ) {
                    if (formState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("TEST CONNECTION")
                    }
                }

                // Test Result View
                formState.testResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result is TestResult.Success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (result is TestResult.Success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (result) {
                                is TestResult.Success -> result.message
                                is TestResult.Failure -> result.message
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result is TestResult.Success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !formState.isSaving
                ) {
                    Text("SAVE PROFILE")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
