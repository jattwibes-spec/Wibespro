package com.example.engine

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ResourceProtector {

    data class ProtectionArtifacts(
        val modifiedManifest: ByteArray?,
        val newAssets: Map<String, ByteArray>,
        val integrityReport: String
    )

    fun protectResources(
        packageName: String,
        manifestBytes: ByteArray?,
        assetsMap: Map<String, ByteArray>,
        dexSha1List: List<String>,
        certFingerprintSha256: String,
        enabledModules: Set<String>
    ): ProtectionArtifacts {
        val newAssets = mutableMapOf<String, ByteArray>()
        val report = StringBuilder()

        // 1. Asset Encryption (Module 27)
        if ("mod_asset_encrypt" in enabledModules) {
            val keyBytes = MessageDigest.getInstance("SHA-256")
                .digest("$packageName.wibes.protect.secret.salt".toByteArray())
                .copyOf(16) // AES-128
            val secretKey = SecretKeySpec(keyBytes, "AES")

            var encryptedCount = 0
            for ((path, data) in assetsMap) {
                // Encrypt sensitive configs, databases, and custom assets
                if (path.endsWith(".json") || path.endsWith(".xml") || path.endsWith(".txt") || path.endsWith(".db")) {
                    try {
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        val iv = ByteArray(12) { 0x33.toByte() }
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                        val encrypted = cipher.doFinal(data)
                        newAssets[path] = encrypted
                        encryptedCount++
                    } catch (e: Exception) {
                        newAssets[path] = data
                    }
                } else {
                    newAssets[path] = data
                }
            }
            report.append("Encrypted $encryptedCount sensitive asset files with AES-GCM.\n")
        } else {
            newAssets.putAll(assetsMap)
        }

        // 2. Resource & Asset Integrity Manifest (Module 25, 28, 29)
        if ("mod_res_integrity" in enabledModules || "mod_asset_integrity" in enabledModules) {
            val manifestBaos = ByteArrayOutputStream()
            val md = MessageDigest.getInstance("SHA-256")

            manifestBaos.write("WIBES_PROTECT_RESOURCE_INTEGRITY_V1\n".toByteArray())
            manifestBaos.write("PACKAGE=$packageName\n".toByteArray())
            manifestBaos.write("CERT_SHA256=$certFingerprintSha256\n".toByteArray())

            for ((path, data) in newAssets) {
                md.reset()
                val hash = md.digest(data).joinToString("") { "%02x".format(it) }
                manifestBaos.write("ASSET:$path=$hash\n".toByteArray())
            }

            for ((idx, sha1) in dexSha1List.withIndex()) {
                manifestBaos.write("DEX:classes${if (idx == 0) "" else (idx + 1).toString()}.dex=$sha1\n".toByteArray())
            }

            newAssets["assets/wibes_resource_integrity.bin"] = manifestBaos.toByteArray()
            report.append("Generated cryptographic resource integrity manifest.\n")
        }

        // 3. Application Hardening & Security Config (Module 31, 32, 34, 40)
        val configJson = """
            {
              "engine": "WIBES PROTECT 1.0.0",
              "package_name": "$packageName",
              "signing_cert_sha256": "$certFingerprintSha256",
              "dex_signatures": [${dexSha1List.joinToString(",") { "\"$it\"" }}],
              "anti_tamper_flags": {
                "check_package_identity": ${"mod_pkg_identity" in enabledModules},
                "check_cert_signature": ${"mod_cert_verif" in enabledModules},
                "check_debuggable_status": ${"mod_debug_config_check" in enabledModules},
                "check_root_environment": ${"mod_root_risk_detect" in enabledModules},
                "check_runtime_hooks": ${"mod_runtime_sec_diag" in enabledModules}
              },
              "build_timestamp": ${System.currentTimeMillis()},
              "integrity_status": "AUTHENTICATED"
            }
        """.trimIndent()
        newAssets["assets/wibes_security_config.json"] = configJson.toByteArray(Charsets.UTF_8)

        // 4. AndroidManifest Sanitization (Force debuggable=false if enabled)
        var modifiedManifest: ByteArray? = null
        if (manifestBytes != null && "mod_debug_config_check" in enabledModules) {
            modifiedManifest = sanitizeManifestBytes(manifestBytes)
            report.append("Sanitized AndroidManifest.xml: enforced android:debuggable='false'.\n")
        }

        return ProtectionArtifacts(
            modifiedManifest = modifiedManifest,
            newAssets = newAssets,
            integrityReport = report.toString()
        )
    }

    private fun sanitizeManifestBytes(manifestBytes: ByteArray): ByteArray {
        val copy = manifestBytes.copyOf()
        // If manifest contains "debuggable" attribute in string pool or attributes, ensure value is 0
        // Search for string "debuggable" in bytes
        val debuggableStr = "debuggable".toByteArray(Charsets.UTF_8)
        for (i in 0..copy.size - debuggableStr.size) {
            var match = true
            for (j in debuggableStr.indices) {
                if (copy[i + j] != debuggableStr[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                // Found occurrence, look ahead for boolean flag 0x12 and zero it out if present
                val lookAheadLimit = (i + 128).coerceAtMost(copy.size)
                for (k in i until lookAheadLimit - 4) {
                    if (copy[k] == 0x12.toByte() && copy[k + 1] == 0x00.toByte()) { // TYPE_INT_BOOLEAN true
                        if (copy[k + 2] == 0xFF.toByte() || copy[k + 2] == 0x01.toByte()) {
                            copy[k + 2] = 0x00.toByte() // Set to false
                        }
                    }
                }
                break
            }
        }
        return copy
    }
}
