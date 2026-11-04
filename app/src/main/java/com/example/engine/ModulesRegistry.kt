package com.example.engine

import com.example.model.ProtectionCategory
import com.example.model.ProtectionModule
import com.example.model.ProtectionProfile

object ModulesRegistry {

    fun getAllModules(): List<ProtectionModule> = listOf(
        // Category A: Code Obfuscation (1-15)
        ProtectionModule(
            id = "mod_class_obf",
            number = 1,
            name = "Class Name Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Renames internal and utility classes to opaque short unprintable or pseudo-random identifiers, breaking class hierarchy reverse-engineering.",
            compatibilityInfo = "Preserves Android components (Activities, Services, Receivers, Providers) registered in AndroidManifest.xml.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_method_obf",
            number = 2,
            name = "Method Name Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Obfuscates non-overridden member methods and private invocations to identical overloaded signatures with distinct descriptor types.",
            compatibilityInfo = "Preserves JNI native methods, lifecycle callbacks, and reflection targets.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_field_obf",
            number = 3,
            name = "Field Name Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Transforms private, protected, and package-private field identifiers into single-character or confusable symbol sequences.",
            compatibilityInfo = "Excludes serializable fields and serialized JSON model fields with explicit annotations.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_package_obf",
            number = 4,
            name = "Package Name Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Flattens and relocates internal sub-package hierarchies into a single compact root package namespace.",
            compatibilityInfo = "Preserves the root application manifest package name and exported component paths.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_string_encrypt",
            number = 5,
            name = "String Encryption",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Encrypts static string literals using dynamic XOR/AES key schedules with automated in-memory decryption routines.",
            compatibilityInfo = "Compatible with all Android versions; runs transparently without external dependencies.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_int_const_obf",
            number = 6,
            name = "Integer Constant Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Replaces raw integer numeric literals with complex bitwise arithmetic equations (shifts, XOR chains, negations).",
            compatibilityInfo = "Universal compatibility. No runtime overhead on modern ART JIT/AOT engines.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_bool_const_obf",
            number = 7,
            name = "Boolean Constant Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Substitutes literal boolean true/false flags with opaque comparison predicates derived from runtime environmental invariants.",
            compatibilityInfo = "Universal compatibility.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_control_flow_obf",
            number = 8,
            name = "Control Flow Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Flattens control flow graphs by inserting opaque conditional predicates, dispatcher loops, and non-deterministic branch jumps.",
            compatibilityInfo = "Designed for ART compiler compatibility while hindering static decompilers (JADX, CFR, Ghidra).",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_arithmetic_obf",
            number = 9,
            name = "Arithmetic Expression Obfuscation",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Applies Mixed Boolean-Arithmetic (MBA) rewriting to algebraic calculations, expanding simple expressions into cryptographically complex equations.",
            compatibilityInfo = "Universal compatibility for all DEX bytecode.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_dead_code_insert",
            number = 10,
            name = "Dead Code Insertion",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Injects unreachable bogus bytecode basic blocks and synthetic dummy branches to confuse automated call-graph analysis.",
            compatibilityInfo = "Preserves deterministic execution without altering valid application output.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_method_split",
            number = 11,
            name = "Method Splitting",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Fragments large routines into interconnected subroutines linked via synthetic trampoline proxies.",
            compatibilityInfo = "Complies with DEX 64K method index bounds and multidex partitioning.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_class_restruct",
            number = 12,
            name = "Class Restructuring",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Synthesizes interface proxies and hoists member variables across inheritance boundaries to dismantle semantic structure.",
            compatibilityInfo = "Maintains polymorphic method dispatch and constructor contract compliance.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_debug_strip",
            number = 13,
            name = "Debug Metadata Stripping",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Removes SourceFile attributes, LineNumberTable offsets, and LocalVariableTable parameter names from all DEX classes.",
            compatibilityInfo = "Greatly reduces APK size and prevents debuggers from attaching source symbols.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_annotation_reduce",
            number = 14,
            name = "Annotation Metadata Reduction",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Strips non-runtime annotations, source-retention meta-tags, and build-time metadata traces.",
            compatibilityInfo = "Preserves runtime-reflective annotations required by frameworks (Room, Moshi, Retrofit).",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_symbol_rename",
            number = 15,
            name = "Symbol Renaming",
            category = ProtectionCategory.CODE_OBFUSCATION,
            description = "Employs confusable unicode characters and identical overload descriptors to maximize disassembler ambiguity.",
            compatibilityInfo = "Verified compatible with Android ART runtime and DEX bytecode specs.",
            isCore = true
        ),

        // Category B: DEX Protection (16-23)
        ProtectionModule(
            id = "mod_dex_integrity",
            number = 16,
            name = "DEX Integrity Verification",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Calculates and hardens Adler32 checksums and SHA-1 digest headers for all DEX containers in the APK.",
            compatibilityInfo = "Crucial for preventing DEX header corruption while sealing tamper detection.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_multidex_compat",
            number = 17,
            name = "Multidex Compatibility Protection",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Guarantees primary classes.dex contains Application class and initial launch components during transformation.",
            compatibilityInfo = "Essential for multidex applications (classes2.dex, classes3.dex).",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_dex_meta_protect",
            number = 18,
            name = "DEX Metadata Protection",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Normalizes DEX header magic, endianness tags, and link section offsets to thwart header-tampering attacks.",
            compatibilityInfo = "Compatible with Android 5.0 through Android 15.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_dex_struct_transform",
            number = 19,
            name = "DEX Structural Transformation",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Re-indexes string ID pools, type IDs, and proto descriptors into scrambled lexicographical order.",
            compatibilityInfo = "Maintains binary format compliance with Android Runtime verifier.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_method_bytecode_obf",
            number = 20,
            name = "Method-level Bytecode Obfuscation",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Injects pseudo-instructions, redundant register moves, and NOP sequences within method bytecode payloads.",
            compatibilityInfo = "Ensures zero functional side-effects on Dalvik/ART virtual machine execution.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_dex_enc_auth",
            number = 21,
            name = "Per-build Authenticated DEX Encryption",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Packages secondary DEX payloads into AES-256-GCM authenticated cipher streams verified with HMAC-SHA256.",
            compatibilityInfo = "Applied to secondary DEX payloads; primary launcher remains bootstrap executable.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_dex_runtime_restore",
            number = 22,
            name = "Runtime DEX Restoration Stub",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Configures dynamic in-memory classloader bootstrap routines that securely restore protected bytecode at startup.",
            compatibilityInfo = "Verified on Android ART classloader architectures.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_dex_checksum_verif",
            number = 23,
            name = "DEX Checksum Verification",
            category = ProtectionCategory.DEX_PROTECTION,
            description = "Embeds pre-computed cryptographic checksums into an immutable integrity manifest checked upon application boot.",
            compatibilityInfo = "Universal compatibility across all Android architectures.",
            isCore = true
        ),

        // Category C: Resource Protection (24-30)
        ProtectionModule(
            id = "mod_res_name_obf",
            number = 24,
            name = "Resource Name Obfuscation",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Scrambles internal resource path names in res/ and layout identifiers while preserving R.java integer mappings.",
            compatibilityInfo = "Preserves Android app icon, theme drawables, and system-facing XML tags.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_res_integrity",
            number = 25,
            name = "Resource Integrity Verification",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Generates a SHA-256 integrity map for resources.arsc and individual drawable XML/PNG assets.",
            compatibilityInfo = "Zero overhead during normal asset loading.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_xml_opt",
            number = 26,
            name = "XML Resource Optimization",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Strips developer comments, whitespace tokens, and unused attribute keys from binary AXML resources.",
            compatibilityInfo = "Standard Android AXML format preserved.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_asset_encrypt",
            number = 27,
            name = "Asset Encryption",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Encrypts sensitive data stored within the assets/ folder with AES-GCM and embeds a secure dynamic key derivation matrix.",
            compatibilityInfo = "Protects config files, embedded databases, and sensitive machine learning models.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_asset_integrity",
            number = 28,
            name = "Asset Integrity Verification",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Creates a tamper-proof cryptographic manifest covering all files in the assets/ directory.",
            compatibilityInfo = "Universal compatibility.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_res_tamper_detect",
            number = 29,
            name = "Resource Tamper Detection",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Embeds self-checking verification hashes into compiled resource tables to detect runtime asset patching.",
            compatibilityInfo = "Detects resource replacer tools and UI spoofing frameworks.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_config_integrity",
            number = 30,
            name = "Configuration Integrity Protection",
            category = ProtectionCategory.RESOURCE_PROTECTION,
            description = "Binds AndroidManifest.xml package declarations and permissions to a cryptographic digital seal.",
            compatibilityInfo = "Prevents attackers from altering exported attributes or adding unauthorized permissions.",
            isCore = true
        ),

        // Category D: Application Hardening (31-40)
        ProtectionModule(
            id = "mod_pkg_identity",
            number = 31,
            name = "Package Identity Verification",
            category = ProtectionCategory.APP_HARDENING,
            description = "Hardens bytecode against package cloning by embedding immutable package signature checks into the core engine.",
            compatibilityInfo = "Prevents repackaging under a modified applicationId.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_cert_verif",
            number = 32,
            name = "Signing Certificate Verification",
            category = ProtectionCategory.APP_HARDENING,
            description = "Pins the SHA-256 fingerprint of the release signing certificate, aborting execution if signed by third-party keys.",
            compatibilityInfo = "Crucial defense against APK crack/patch distribution.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_runtime_integrity",
            number = 33,
            name = "Runtime Integrity Checks",
            category = ProtectionCategory.APP_HARDENING,
            description = "Validates the ZIP central directory hash and CRC32 table against a sealed cryptographic digest at runtime.",
            compatibilityInfo = "Zero impact on standard application performance.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_debug_config_check",
            number = 34,
            name = "Debuggable Configuration Checks",
            category = ProtectionCategory.APP_HARDENING,
            description = "Enforces non-debuggable execution and automatically sanitizes android:debuggable attributes to false.",
            compatibilityInfo = "Standard best practice for production application releases.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_native_lib_integrity",
            number = 35,
            name = "Native Library Integrity Verification",
            category = ProtectionCategory.APP_HARDENING,
            description = "Generates SHA-256 signatures for compiled .so binaries across all supported ABI architectures (arm64, v7a, x86).",
            compatibilityInfo = "Detects injection of rogue native hook libraries (Frida, Substrate).",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_sec_config_handling",
            number = 36,
            name = "Secure Configuration Handling",
            category = ProtectionCategory.APP_HARDENING,
            description = "Validates Network Security Configuration to disallow unencrypted cleartext HTTP traffic.",
            compatibilityInfo = "Aligns with Google Play security and network transmission policies.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_root_risk_detect",
            number = 37,
            name = "Root Environment Risk Detection",
            category = ProtectionCategory.APP_HARDENING,
            description = "Injects defensive heuristic checks for su binaries, test-keys build tags, Magisk paths, and Superuser presence.",
            compatibilityInfo = "Configurable defensive checks that alert or gracefully handle compromised device environments.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_runtime_sec_diag",
            number = 38,
            name = "Runtime Security Diagnostics",
            category = ProtectionCategory.APP_HARDENING,
            description = "Performs runtime scanning for dynamic instrumentation frameworks (Frida server ports, Xposed bridge, Cydia).",
            compatibilityInfo = "Protects intellectual property against dynamic runtime hooking.",
            isCore = false
        ),
        ProtectionModule(
            id = "mod_apk_mod_detect",
            number = 39,
            name = "APK Modification Detection",
            category = ProtectionCategory.APP_HARDENING,
            description = "Monitors APK timestamp deltas, ZIP comments, and manifest hash deviations to flag post-build alterations.",
            compatibilityInfo = "Universal compatibility.",
            isCore = true
        ),
        ProtectionModule(
            id = "mod_build_artifact_verif",
            number = 40,
            name = "Build Artifact Integrity Verification",
            category = ProtectionCategory.APP_HARDENING,
            description = "Compiles an authenticated build receipt containing SHA-256 hashes of all applied modules and protection parameters.",
            compatibilityInfo = "Provides cryptographically verifiable audit trails for enterprise compliance.",
            isCore = true
        )
    )

    fun getModulesForProfile(profile: ProtectionProfile): List<ProtectionModule> {
        val all = getAllModules()
        return when (profile) {
            ProtectionProfile.BASIC -> all.map { mod ->
                val enabled = mod.number in listOf(1, 4, 5, 13, 14, 15, 16, 17, 18, 23, 25, 26, 30, 31, 32, 34, 36)
                mod.copy(isEnabled = enabled)
            }
            ProtectionProfile.ADVANCED -> all.map { mod ->
                val enabled = mod.number in listOf(
                    1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 13, 14, 15,
                    16, 17, 18, 19, 23, 24, 25, 26, 27, 28, 30, 31, 32, 33, 34, 36, 39, 40
                )
                mod.copy(isEnabled = enabled)
            }
            ProtectionProfile.EXTREME -> all.map { mod ->
                val enabled = mod.number !in listOf(21, 22) // All except dynamic dex packing
                mod.copy(isEnabled = enabled)
            }
            ProtectionProfile.MAXIMUM -> all.map { mod ->
                mod.copy(isEnabled = true)
            }
            ProtectionProfile.CUSTOM -> all.map { mod ->
                mod.copy(isEnabled = mod.isCore)
            }
        }
    }
}
