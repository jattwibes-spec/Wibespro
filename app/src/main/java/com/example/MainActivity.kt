package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.dialogs.AboutAdminDialog
import com.example.ui.dialogs.AboutAppDialog
import com.example.ui.dialogs.CompletionResultDialog
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SigningScreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.WibesProtectTheme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WibesProtectTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var showAboutApp by remember { mutableStateOf(false) }
                var showAboutAdmin by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                var pendingExportFile by remember { mutableStateOf<File?>(null) }

                // Storage Access Framework: Browse APK
                val browseApkLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {}
                        viewModel.selectApk(uri)
                    }
                }

                // Storage Access Framework: Browse Keystore
                val browseKeystoreLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        // Copy to app files for signing access
                        try {
                            val dest = File(filesDir, "imported_key_${System.currentTimeMillis()}.p12")
                            contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(dest).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            viewModel.updateKeystoreConfig(
                                uiState.keystoreConfig.copy(keystorePath = dest.absolutePath)
                            )
                            Toast.makeText(this@MainActivity, "Keystore file imported successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Failed to load keystore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Storage Access Framework: Save / Download APK
                val saveApkLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
                ) { uri: Uri? ->
                    val fileToSave = pendingExportFile
                    if (uri != null && fileToSave != null && fileToSave.exists()) {
                        try {
                            contentResolver.openOutputStream(uri)?.use { output ->
                                FileInputStream(fileToSave).use { input ->
                                    input.copyTo(output)
                                }
                            }
                            Toast.makeText(this@MainActivity, "APK saved to device storage successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Failed to save APK: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // BackHandler
                BackHandler(enabled = uiState.activeTab != 0 || showSettings) {
                    if (showSettings) {
                        showSettings = false
                    } else {
                        viewModel.selectTab(0)
                    }
                }

                // Dialogs
                if (showAboutApp) {
                    AboutAppDialog(onDismiss = { showAboutApp = false })
                }

                if (showAboutAdmin) {
                    AboutAdminDialog(onDismiss = { showAboutAdmin = false })
                }

                uiState.lastResult?.let { result ->
                    CompletionResultDialog(
                        result = result,
                        onDismiss = { viewModel.dismissResultDialog() },
                        onRequestSaveApk = { file ->
                            pendingExportFile = file
                            saveApkLauncher.launch(file.name)
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    bottomBar = {
                        if (!showSettings) {
                            WibesBottomBar(
                                activeTab = uiState.activeTab,
                                onSelectTab = { viewModel.selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (showSettings) {
                            SettingsScreen(
                                onOpenAboutApp = { showAboutApp = true },
                                onOpenAboutAdmin = { showAboutAdmin = true },
                                onClearHistory = { viewModel.loadHistory() }
                            )
                        } else {
                            when (uiState.activeTab) {
                                0 -> DashboardScreen(
                                    uiState = uiState,
                                    onBrowseApk = {
                                        browseApkLauncher.launch(
                                            arrayOf(
                                                "application/vnd.android.package-archive",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    },
                                    onLoadDemoApk = { viewModel.loadSampleApk() },
                                    onSelectProfile = { viewModel.selectProfile(it) },
                                    onStartProtection = { viewModel.startProtection() },
                                    onClearLogs = { viewModel.clearLogs() },
                                    onOpenAboutApp = { showAboutApp = true },
                                    onOpenAboutAdmin = { showAboutAdmin = true },
                                    onOpenSettings = { showSettings = true },
                                    onNavigateToAnalyzer = { viewModel.selectTab(1) },
                                    onNavigateToModules = { viewModel.selectTab(2) }
                                )
                                1 -> AnalyzerScreen(
                                    apkInfo = uiState.selectedApkInfo,
                                    onLoadDemo = { viewModel.loadSampleApk() },
                                    onBrowse = {
                                        browseApkLauncher.launch(
                                            arrayOf(
                                                "application/vnd.android.package-archive",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }
                                )
                                2 -> ModulesScreen(
                                    modules = uiState.modules,
                                    currentProfile = uiState.currentProfile,
                                    onToggleModule = { id, enabled -> viewModel.toggleModule(id, enabled) },
                                    onSelectProfile = { viewModel.selectProfile(it) }
                                )
                                3 -> SigningScreen(
                                    config = uiState.keystoreConfig,
                                    onUpdateConfig = { viewModel.updateKeystoreConfig(it) },
                                    onBrowseKeystoreFile = {
                                        browseKeystoreLauncher.launch(
                                            arrayOf(
                                                "application/x-pkcs12",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }
                                )
                                4 -> HistoryScreen(
                                    history = uiState.history,
                                    onLoadSample = { viewModel.loadSampleApk() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WibesBottomBar(
    activeTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val items = listOf(
        Triple(0, "Dashboard", Icons.Default.Shield),
        Triple(1, "Analyzer", Icons.Default.Security),
        Triple(2, "Modules", Icons.Default.Code),
        Triple(3, "Signing", Icons.Default.Key),
        Triple(4, "History", Icons.Default.History)
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        items.forEach { (index, title, icon) ->
            val isSelected = activeTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = ElectricViolet,
                    indicatorColor = PrimaryPurple,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${title.lowercase()}")
            )
        }
    }
}
