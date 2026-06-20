package com.sshvan.tunnelmanager.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.sshvan.tunnelmanager.presentation.ui.navigation.AppNavGraph
import com.sshvan.tunnelmanager.presentation.ui.theme.SSHTunnelManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SSHTunnelManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                    
                    var showCrashDialog by remember { mutableStateOf(false) }
                    var crashLog by remember { mutableStateOf("") }
                    
                    LaunchedEffect(Unit) {
                        val crashFile = java.io.File(filesDir, "crash.txt")
                        if (crashFile.exists()) {
                            crashLog = crashFile.readText()
                            showCrashDialog = true
                        }
                    }
                    
                    if (showCrashDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { 
                                showCrashDialog = false
                                java.io.File(filesDir, "crash.txt").delete()
                            },
                            title = { Text("App Crashed Previously") },
                            text = { 
                                Column {
                                    Text("Please copy this error and send it to the developer:")
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = crashLog,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.height(200.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    com.sshvan.tunnelmanager.util.NetworkUtils.copyToClipboard(this@MainActivity, "Crash Log", crashLog)
                                }) {
                                    Text("Copy Error")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = {
                                    showCrashDialog = false
                                    java.io.File(filesDir, "crash.txt").delete()
                                }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
