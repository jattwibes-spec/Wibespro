package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ApkAnalyzer
import com.example.data.ProtectionHistoryManager
import com.example.engine.ModulesRegistry
import com.example.engine.ProtectionEngine
import com.example.model.ApkInfo
import com.example.model.KeystoreConfig
import com.example.model.LogLevel
import com.example.model.ProtectionHistoryItem
import com.example.model.ProtectionLog
import com.example.model.ProtectionModule
import com.example.model.ProtectionProfile
import com.example.model.ProtectionProgress
import com.example.model.ProtectionResult
import com.example.sample.SampleApkGenerator
import com.example.signing.SigningEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class MainUiState(
    val selectedApkUri: Uri? = null,
    val selectedApkInfo: ApkInfo? = null,
    val isAnalyzing: Boolean = false,
    val analysisError: String? = null,
    val currentProfile: ProtectionProfile = ProtectionProfile.MAXIMUM,
    val modules: List<ProtectionModule> = ModulesRegistry.getModulesForProfile(ProtectionProfile.MAXIMUM),
    val keystoreConfig: KeystoreConfig = KeystoreConfig(),
    val progress: ProtectionProgress = ProtectionProgress(),
    val logs: List<ProtectionLog> = listOf(
        ProtectionLog(
            level = LogLevel.INFO,
            message = "WIBES PROTECT initialized. Engine v2.4.0-CORE ready."
        ),
        ProtectionLog(
            level = LogLevel.INFO,
            message = "Select an APK file or load the sample demo application to begin."
        )
    ),
    val lastResult: ProtectionResult? = null,
    val history: List<ProtectionHistoryItem> = emptyList(),
    val activeTab: Int = 0 // 0: Dashboard, 1: Analyzer, 2: Modules, 3: Signing, 4: History
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val analyzer = ApkAnalyzer(context)
    private val engine = ProtectionEngine(context, analyzer)
    private val signingEngine = SigningEngine(context)
    private val sampleGenerator = SampleApkGenerator(context)
    private val historyManager = ProtectionHistoryManager(context)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun loadHistory() {
        _uiState.update { it.copy(history = historyManager.getHistory()) }
    }

    fun selectApk(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, analysisError = null, selectedApkUri = uri) }
            addLog(LogLevel.INFO, "Analyzing selected APK: $uri")
            try {
                val info = analyzer.analyze(uri)
                _uiState.update {
                    it.copy(
                        selectedApkInfo = info,
                        isAnalyzing = false
                    )
                }
                addLog(LogLevel.SUCCESS, "APK Analyzed: ${info.packageName} (v${info.versionName}) - Size: ${formatSize(info.fileSize)}")
                addLog(LogLevel.INFO, "Extracted ${info.dexFiles.size} DEX files, ${info.permissions.size} permissions, ${info.auditFindings.size} audit items.")
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, analysisError = e.message) }
                addLog(LogLevel.ERROR, "Failed to analyze APK: ${e.message}")
            }
        }
    }

    fun loadSampleApk() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, analysisError = null) }
            addLog(LogLevel.INFO, "Generating & loading valid test target APK into sandbox...")
            try {
                val sampleFile = sampleGenerator.getOrCreateSampleApk()
                val uri = Uri.fromFile(sampleFile)
                val info = analyzer.analyze(uri)
                _uiState.update {
                    it.copy(
                        selectedApkUri = uri,
                        selectedApkInfo = info,
                        isAnalyzing = false
                    )
                }
                addLog(LogLevel.SUCCESS, "Loaded sample target APK: ${info.packageName} (${formatSize(info.fileSize)})")
                addLog(LogLevel.INFO, "Ready for real 16-step obfuscation, rebuilding, and digital signing.")
            } catch (e: Exception) {
                _uiState.update { it.copy(isAnalyzing = false, analysisError = e.message) }
                addLog(LogLevel.ERROR, "Failed to load sample APK: ${e.message}")
            }
        }
    }

    fun selectProfile(profile: ProtectionProfile) {
        val newModules = ModulesRegistry.getModulesForProfile(profile)
        _uiState.update {
            it.copy(
                currentProfile = profile,
                modules = newModules
            )
        }
        addLog(LogLevel.INFO, "Profile switched to: ${profile.displayName} (${newModules.count { m -> m.isEnabled }} modules active)")
    }

    fun toggleModule(moduleId: String, enabled: Boolean) {
        val updated = _uiState.value.modules.map { mod ->
            if (mod.id == moduleId) mod.copy(isEnabled = enabled) else mod
        }
        _uiState.update {
            it.copy(
                modules = updated,
                currentProfile = ProtectionProfile.CUSTOM
            )
        }
    }

    fun updateKeystoreConfig(config: KeystoreConfig) {
        _uiState.update { it.copy(keystoreConfig = config) }
        addLog(LogLevel.INFO, "Keystore config updated: Mode ${config.mode.name}, Alias '${config.keyAlias}'")
    }

    fun startProtection() {
        val state = _uiState.value
        val apkUri = state.selectedApkUri ?: return

        if (state.progress.isRunning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progress = ProtectionProgress(isRunning = true, step = 1, stepName = "Initializing Pipeline", percentage = 3f),
                    lastResult = null
                )
            }

            try {
                val result = engine.executePipeline(
                    apkUri = apkUri,
                    profile = state.currentProfile,
                    modules = state.modules,
                    keystoreConfig = state.keystoreConfig,
                    onLog = { log -> addLog(log.level, log.message) },
                    onProgress = { progress -> _uiState.update { it.copy(progress = progress) } }
                )

                // Save to history
                val historyItem = ProtectionHistoryItem(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    originalApkName = result.originalApkName,
                    protectedApkName = result.protectedApkName,
                    originalSize = result.originalSize,
                    protectedSize = result.protectedSize,
                    profile = result.profileName,
                    appliedCount = result.appliedModulesCount,
                    durationMs = result.processingDurationMs,
                    outputFilePath = result.outputFilePath,
                    outputFileUriString = result.outputFileUriString,
                    certFingerprintSha256 = result.certFingerprintSha256
                )
                historyManager.addItem(historyItem)
                loadHistory()

                _uiState.update {
                    it.copy(
                        lastResult = result,
                        progress = it.progress.copy(isRunning = false, isCompleted = true, percentage = 100f, stepName = "Completed")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        progress = it.progress.copy(isRunning = false, isFailed = true, errorMessage = e.message)
                    )
                }
            }
        }
    }

    fun clearLogs() {
        _uiState.update {
            it.copy(
                logs = listOf(
                    ProtectionLog(
                        level = LogLevel.INFO,
                        message = "Logs cleared by user."
                    )
                )
            )
        }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(lastResult = null) }
    }

    private fun addLog(level: LogLevel, message: String) {
        val log = ProtectionLog(level = level, message = message)
        _uiState.update {
            val list = it.logs.toMutableList()
            list.add(log)
            it.copy(logs = list.takeLast(250))
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
