package com.example.model

import android.graphics.Bitmap

data class ApkInfo(
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val iconBitmap: Bitmap? = null,
    val permissions: List<PermissionInfo> = emptyList(),
    val activities: List<ComponentInfo> = emptyList(),
    val services: List<ComponentInfo> = emptyList(),
    val receivers: List<ComponentInfo> = emptyList(),
    val providers: List<ComponentInfo> = emptyList(),
    val isDebuggable: Boolean = false,
    val allowsBackup: Boolean = true,
    val cleartextTrafficAllowed: Boolean = true,
    val dexFiles: List<DexInfo> = emptyList(),
    val nativeLibs: List<NativeLibInfo> = emptyList(),
    val signatures: List<SignatureInfo> = emptyList(),
    val sha256Hex: String = "",
    val md5Hex: String = "",
    val totalEntriesCount: Int = 0,
    val auditFindings: List<SecurityAuditFinding> = emptyList()
)

data class PermissionInfo(
    val name: String,
    val simpleName: String,
    val isDangerous: Boolean,
    val description: String = ""
)

data class ComponentInfo(
    val name: String,
    val simpleName: String,
    val isExported: Boolean,
    val permission: String? = null
)

data class DexInfo(
    val name: String,
    val size: Long,
    val classCount: Int,
    val methodCount: Int,
    val fieldCount: Int,
    val stringPoolSize: Int,
    val checksumValid: Boolean,
    val sha1Hex: String
)

data class NativeLibInfo(
    val abi: String,
    val fileName: String,
    val size: Long,
    val sha256: String
)

data class SignatureInfo(
    val scheme: String, // V1, V2, V3
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validTo: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val md5Fingerprint: String,
    val sigAlg: String
)

enum class Severity {
    HIGH,
    MEDIUM,
    LOW,
    GOOD
}

data class SecurityAuditFinding(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val category: String,
    val recommendation: String
)
