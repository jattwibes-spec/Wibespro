package com.example.model

enum class ProtectionCategory(val displayName: String, val iconName: String) {
    CODE_OBFUSCATION("Code Obfuscation", "code"),
    DEX_PROTECTION("DEX Protection", "layers"),
    RESOURCE_PROTECTION("Resource Protection", "folder_zip"),
    APP_HARDENING("Application Hardening", "security")
}

enum class ProtectionProfile(
    val displayName: String,
    val tagline: String,
    val description: String
) {
    BASIC(
        "Basic Protection",
        "Essential Obfuscation",
        "Basic code symbol obfuscation, string encryption, debug metadata stripping, resource validation, and integrity checks."
    ),
    ADVANCED(
        "Advanced Protection",
        "Standard Hardening",
        "Includes Basic plus control flow transformations, constant obfuscation, method splitting, asset protection, and DEX integrity verification."
    ),
    EXTREME(
        "Extreme Protection",
        "High-Security Defenses",
        "Includes Advanced plus advanced bytecode transformations, resource tamper detection, certificate pinning verification, native library integrity, and runtime security diagnostics."
    ),
    MAXIMUM(
        "Maximum Protection",
        "Military-Grade Defenses",
        "Enables all 40 protection modules with full DEX authenticated encryption, multi-layered integrity sealing, anti-root, and anti-tamper safeguards."
    ),
    CUSTOM(
        "Custom Protection",
        "User Configured",
        "Granular control over each individual protection module with real-time compatibility checks."
    )
}

data class ProtectionModule(
    val id: String,
    val number: Int,
    val name: String,
    val category: ProtectionCategory,
    val description: String,
    val compatibilityInfo: String,
    val isEnabled: Boolean = false,
    val isApplied: Boolean = false,
    val isCore: Boolean = false
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    DEBUG
}

data class ProtectionLog(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

data class ProtectionProgress(
    val step: Int = 0,
    val totalSteps: Int = 16,
    val stepName: String = "Ready",
    val percentage: Float = 0f,
    val currentModule: String = "",
    val completedCount: Int = 0,
    val totalModules: Int = 0,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

data class ProtectionResult(
    val originalApkName: String,
    val protectedApkName: String,
    val originalSize: Long,
    val protectedSize: Long,
    val profileName: String,
    val appliedModulesCount: Int,
    val appliedModules: List<String>,
    val signingStatus: String,
    val certOwner: String,
    val certFingerprintSha256: String,
    val outputFilePath: String,
    val outputFileUriString: String?,
    val processingDurationMs: Long,
    val verificationPassed: Boolean,
    val securityReport: String
)

enum class KeystoreMode {
    AUTO_GENERATE,
    CUSTOM_NEW,
    IMPORT_EXISTING
}

data class KeystoreConfig(
    val mode: KeystoreMode = KeystoreMode.AUTO_GENERATE,
    val keystorePath: String = "",
    val keystorePassword: String = "wibesprotect",
    val keyAlias: String = "wibes_key",
    val keyPassword: String = "wibesprotect",
    val validityYears: Int = 25,
    val ownerName: String = "WIBES PROTECT Developer",
    val organization: String = "Wibes Security Systems"
)

data class ProtectionHistoryItem(
    val id: String,
    val timestamp: Long,
    val originalApkName: String,
    val protectedApkName: String,
    val originalSize: Long,
    val protectedSize: Long,
    val profile: String,
    val appliedCount: Int,
    val durationMs: Long,
    val outputFilePath: String,
    val outputFileUriString: String?,
    val certFingerprintSha256: String
)
