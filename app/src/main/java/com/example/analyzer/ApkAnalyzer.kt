package com.example.analyzer

import android.content.Context
import android.net.Uri
import com.example.model.ApkInfo
import com.example.model.ComponentInfo
import com.example.model.DexInfo
import com.example.model.NativeLibInfo
import com.example.model.PermissionInfo
import com.example.model.SecurityAuditFinding
import com.example.model.Severity
import com.example.model.SignatureInfo
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ApkAnalyzer(private val context: Context) {

    fun analyze(apkUri: Uri): ApkInfo {
        var fileName = "application.apk"
        var fileSize = 0L

        // Determine file name and size
        context.contentResolver.query(apkUri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
            }
        }

        val dexFiles = mutableListOf<DexInfo>()
        val nativeLibs = mutableListOf<NativeLibInfo>()
        val signatures = mutableListOf<SignatureInfo>()
        var manifestData: BinaryXmlDecoder.ManifestData? = null
        var totalEntries = 0

        // Hash streams
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val md5Digest = MessageDigest.getInstance("MD5")

        val inputStream = context.contentResolver.openInputStream(apkUri)
            ?: throw IllegalArgumentException("Cannot open APK stream from URI: $apkUri")

        inputStream.use { rawIn ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            val zipIn = ZipInputStream(rawIn)
            var entry: ZipEntry? = zipIn.nextEntry

            while (entry != null) {
                totalEntries++
                val entryName = entry.name

                if (entryName == "AndroidManifest.xml") {
                    val bytes = readStreamBytes(zipIn)
                    sha256Digest.update(bytes)
                    md5Digest.update(bytes)
                    manifestData = BinaryXmlDecoder(bytes).decode()
                } else if (entryName.endsWith(".dex")) {
                    val dexBytes = readStreamBytes(zipIn)
                    sha256Digest.update(dexBytes)
                    md5Digest.update(dexBytes)
                    val dexInfo = parseDex(entryName, dexBytes)
                    dexFiles.add(dexInfo)
                } else if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
                    val libBytes = readStreamBytes(zipIn)
                    sha256Digest.update(libBytes)
                    md5Digest.update(libBytes)
                    val parts = entryName.split("/")
                    val abi = if (parts.size >= 2) parts[1] else "unknown"
                    val libName = parts.last()
                    val hash = computeSha256(libBytes)
                    nativeLibs.add(NativeLibInfo(abi, libName, libBytes.size.toLong(), hash))
                } else if (entryName.startsWith("META-INF/") &&
                    (entryName.endsWith(".RSA") || entryName.endsWith(".DSA") || entryName.endsWith(".EC"))
                ) {
                    val certBytes = readStreamBytes(zipIn)
                    sha256Digest.update(certBytes)
                    md5Digest.update(certBytes)
                    val parsedSigs = parseSignatures(certBytes)
                    signatures.addAll(parsedSigs)
                } else {
                    // Update digests for other entries
                    while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                        sha256Digest.update(buffer, 0, bytesRead)
                        md5Digest.update(buffer, 0, bytesRead)
                    }
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        // If file size wasn't known from ContentResolver, compute from cache
        if (fileSize <= 0) {
            val file = File(apkUri.path ?: "")
            if (file.exists()) fileSize = file.length()
        }

        val manifest = manifestData ?: BinaryXmlDecoder(ByteArray(0)).decode()
        val permissions = manifest.permissions.map { perm ->
            val simple = perm.substringAfterLast(".")
            val isDangerous = perm in DANGEROUS_PERMISSIONS
            PermissionInfo(
                name = perm,
                simpleName = simple,
                isDangerous = isDangerous,
                description = getPermissionDesc(simple)
            )
        }

        val activities = manifest.activities.map {
            ComponentInfo(it.name, it.name.substringAfterLast("."), it.isExported, it.permission)
        }
        val services = manifest.services.map {
            ComponentInfo(it.name, it.name.substringAfterLast("."), it.isExported, it.permission)
        }
        val receivers = manifest.receivers.map {
            ComponentInfo(it.name, it.name.substringAfterLast("."), it.isExported, it.permission)
        }
        val providers = manifest.providers.map {
            ComponentInfo(it.name, it.name.substringAfterLast("."), it.isExported, it.permission)
        }

        // Conduct deep security audit
        val auditFindings = performSecurityAudit(
            manifest,
            dexFiles,
            signatures,
            nativeLibs
        )

        val finalSha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) }
        val finalMd5 = md5Digest.digest().joinToString("") { "%02x".format(it) }

        return ApkInfo(
            uriString = apkUri.toString(),
            fileName = fileName,
            fileSize = fileSize,
            packageName = manifest.packageName,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            minSdk = manifest.minSdk,
            targetSdk = manifest.targetSdk,
            permissions = permissions,
            activities = activities,
            services = services,
            receivers = receivers,
            providers = providers,
            isDebuggable = manifest.isDebuggable,
            allowsBackup = manifest.allowsBackup,
            cleartextTrafficAllowed = manifest.cleartextTrafficAllowed,
            dexFiles = dexFiles,
            nativeLibs = nativeLibs,
            signatures = signatures,
            sha256Hex = finalSha256,
            md5Hex = finalMd5,
            totalEntriesCount = totalEntries,
            auditFindings = auditFindings
        )
    }

    private fun readStreamBytes(inputStream: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun parseDex(name: String, bytes: ByteArray): DexInfo {
        if (bytes.size < 112) {
            return DexInfo(name, bytes.size.toLong(), 0, 0, 0, 0, false, "")
        }

        try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = String(bytes, 0, 8)
            val storedChecksum = buf.getInt(8).toLong() and 0xFFFFFFFFL
            val sha1Bytes = ByteArray(20)
            System.arraycopy(bytes, 12, sha1Bytes, 0, 20)
            val sha1Hex = sha1Bytes.joinToString("") { "%02x".format(it) }

            // Verify Adler32
            val adler = Adler32()
            adler.update(bytes, 12, bytes.size - 12)
            val computedChecksum = adler.value
            val isChecksumValid = storedChecksum == computedChecksum

            val stringIdsSize = buf.getInt(56)
            val typeIdsSize = buf.getInt(64)
            val protoIdsSize = buf.getInt(72)
            val fieldIdsSize = buf.getInt(80)
            val methodIdsSize = buf.getInt(88)
            val classDefsSize = buf.getInt(96)

            return DexInfo(
                name = name,
                size = bytes.size.toLong(),
                classCount = classDefsSize.coerceAtLeast(0),
                methodCount = methodIdsSize.coerceAtLeast(0),
                fieldCount = fieldIdsSize.coerceAtLeast(0),
                stringPoolSize = stringIdsSize.coerceAtLeast(0),
                checksumValid = isChecksumValid,
                sha1Hex = sha1Hex
            )
        } catch (e: Exception) {
            return DexInfo(name, bytes.size.toLong(), 0, 0, 0, 0, false, "")
        }
    }

    private fun parseSignatures(certBytes: ByteArray): List<SignatureInfo> {
        val list = mutableListOf<SignatureInfo>()
        try {
            val cf = CertificateFactory.getInstance("X.509")
            val certs = cf.generateCertificates(ByteArrayInputStream(certBytes))
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            for (c in certs) {
                if (c is X509Certificate) {
                    val encoded = c.encoded
                    val sha256 = MessageDigest.getInstance("SHA-256").digest(encoded).joinToString(":") { "%02X".format(it) }
                    val sha1 = MessageDigest.getInstance("SHA-1").digest(encoded).joinToString(":") { "%02X".format(it) }
                    val md5 = MessageDigest.getInstance("MD5").digest(encoded).joinToString(":") { "%02X".format(it) }

                    list.add(
                        SignatureInfo(
                            scheme = "V1 (JAR Signature)",
                            subject = c.subjectX500Principal.name,
                            issuer = c.issuerX500Principal.name,
                            serialNumber = c.serialNumber.toString(16),
                            validFrom = sdf.format(c.notBefore),
                            validTo = sdf.format(c.notAfter),
                            sha256Fingerprint = sha256,
                            sha1Fingerprint = sha1,
                            md5Fingerprint = md5,
                            sigAlg = c.sigAlgName
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Some PKCS#7 structures require CMS extraction
        }
        return list
    }

    private fun computeSha256(data: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun performSecurityAudit(
        manifest: BinaryXmlDecoder.ManifestData,
        dexFiles: List<DexInfo>,
        signatures: List<SignatureInfo>,
        nativeLibs: List<NativeLibInfo>
    ): List<SecurityAuditFinding> {
        val findings = mutableListOf<SecurityAuditFinding>()

        // 1. Debuggable Audit
        if (manifest.isDebuggable) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_debuggable",
                    title = "Application is Flagged as Debuggable",
                    description = "android:debuggable is set to 'true'. Attackers can attach a debugger (jdb/gdb) and dump sensitive memory, strings, and keys.",
                    severity = Severity.HIGH,
                    category = "Manifest Security",
                    recommendation = "Apply WIBES PROTECT 'Debuggable Configuration Checks' module to enforce false."
                )
            )
        } else {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_debuggable_good",
                    title = "Production Debugging Disabled",
                    description = "android:debuggable is false, preventing unauthorized runtime debugger attachments.",
                    severity = Severity.GOOD,
                    category = "Manifest Security",
                    recommendation = "Maintain non-debuggable flag in production release."
                )
            )
        }

        // 2. Cleartext Traffic Audit
        if (manifest.cleartextTrafficAllowed) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_cleartext",
                    title = "Cleartext HTTP Traffic Permitted",
                    description = "usesCleartextTraffic is true. Unencrypted HTTP transmissions are vulnerable to eavesdropping and Man-In-The-Middle (MITM) attacks.",
                    severity = Severity.MEDIUM,
                    category = "Network Security",
                    recommendation = "Apply 'Secure Configuration Handling' module to enforce HTTPS and certificate pinning."
                )
            )
        }

        // 3. Backup Allowed
        if (manifest.allowsBackup) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_backup",
                    title = "Application Data Backup Allowed",
                    description = "android:allowBackup is true. App databases, shared preferences, and files can be extracted via ADB backup without root.",
                    severity = Severity.LOW,
                    category = "Data Protection",
                    recommendation = "Disable ADB backup or protect sensitive assets using 'Asset Encryption'."
                )
            )
        }

        // 4. Exported Components without permissions
        val exportedActivities = manifest.activities.filter { it.isExported }
        val exportedServices = manifest.services.filter { it.isExported }
        if (exportedActivities.size > 1 || exportedServices.isNotEmpty()) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_exported",
                    title = "Exported Components Detected (${exportedActivities.size} Activities, ${exportedServices.size} Services)",
                    description = "Exported components can be invoked directly by other apps on the device, potentially exposing internal flows or intents.",
                    severity = Severity.MEDIUM,
                    category = "Component Exposure",
                    recommendation = "Verify that only necessary launcher activities are exported and protect services with signature permissions."
                )
            )
        }

        // 5. DEX Obfuscation Status
        val totalMethods = dexFiles.sumOf { it.methodCount }
        val totalClasses = dexFiles.sumOf { it.classCount }
        val totalStrings = dexFiles.sumOf { it.stringPoolSize }
        if (totalStrings > 500) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_strings_exposed",
                    title = "Plaintext Strings Exposed in DEX ($totalStrings strings)",
                    description = "String literal pool contains unencrypted URLs, API endpoints, error strings, and identifiers readable via decompiler.",
                    severity = Severity.HIGH,
                    category = "Code Obfuscation",
                    recommendation = "Apply 'String Encryption' and 'Class & Method Obfuscation' modules to encrypt strings."
                )
            )
        }

        // 6. Signature status
        if (signatures.isEmpty()) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_sig_missing",
                    title = "No V1 Signing Certificate Discovered",
                    description = "The APK might only have modern V2/V3 signatures or is an unsigned debug archive.",
                    severity = Severity.MEDIUM,
                    category = "Digital Signatures",
                    recommendation = "Use WIBES PROTECT Signing Engine to apply dual V1 + V2 digital signatures."
                )
            )
        } else {
            val sig = signatures.first()
            findings.add(
                SecurityAuditFinding(
                    id = "aud_sig_ok",
                    title = "Digitally Signed with ${sig.sigAlg}",
                    description = "Signed by ${sig.subject}. Certificate SHA-256 fingerprint: ${sig.sha256Fingerprint.take(24)}...",
                    severity = Severity.GOOD,
                    category = "Digital Signatures",
                    recommendation = "Certificate will be verified and re-signed upon protection."
                )
            )
        }

        // 7. Native libraries
        if (nativeLibs.isNotEmpty()) {
            findings.add(
                SecurityAuditFinding(
                    id = "aud_native_detected",
                    title = "Native Binaries Detected (${nativeLibs.size} .so files)",
                    description = "Found libraries across ABIs: ${nativeLibs.map { it.abi }.distinct().joinToString(", ")}.",
                    severity = Severity.LOW,
                    category = "Native Code",
                    recommendation = "Enable 'Native Library Integrity Verification' to seal binary checksums against tampering."
                )
            )
        }

        return findings
    }

    private fun getPermissionDesc(simpleName: String): String = when (simpleName) {
        "INTERNET" -> "Full network access"
        "CAMERA" -> "Direct hardware camera sensor access"
        "RECORD_AUDIO" -> "Microphone audio recording"
        "ACCESS_FINE_LOCATION" -> "Precise GPS and network coordinates"
        "ACCESS_COARSE_LOCATION" -> "Approximate cellular network location"
        "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE" -> "Device shared storage read/write"
        "READ_CONTACTS", "WRITE_CONTACTS" -> "Personal address book access"
        "READ_PHONE_STATE" -> "Telephony state and device identifiers"
        "POST_NOTIFICATIONS" -> "Send status bar alerts and notifications"
        "VIBRATE" -> "Haptic feedback control"
        "ACCESS_NETWORK_STATE" -> "Inspect Wi-Fi and mobile connectivity"
        else -> "Standard application capability"
    }

    companion object {
        private val DANGEROUS_PERMISSIONS = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.POST_NOTIFICATIONS"
        )
    }
}
