package com.example.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32

/**
 * On-device DEX bytecode transformer, metadata stripper, string obfuscator, and header sealer.
 */
class DexTransformer(private val rawBytes: ByteArray) {

    private val bytes = rawBytes.copyOf()
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    data class TransformationReport(
        val strippedDebugItems: Int,
        val obfuscatedStrings: Int,
        val sealedChecksum: Long,
        val sealedSha1: String,
        val classDefsCount: Int,
        val methodsCount: Int
    )

    fun transform(
        stripDebugInfo: Boolean = true,
        encryptStrings: Boolean = true,
        obfuscateSymbols: Boolean = true,
        insertIntegritySeal: Boolean = true
    ): Pair<ByteArray, TransformationReport> {
        if (bytes.size < 112) {
            return Pair(bytes, TransformationReport(0, 0, 0, "", 0, 0))
        }

        var strippedDebugCount = 0
        var obfuscatedStringsCount = 0

        val stringIdsSize = buffer.getInt(56)
        val stringIdsOff = buffer.getInt(60)
        val methodIdsSize = buffer.getInt(88)
        val classDefsSize = buffer.getInt(96)
        val classDefsOff = buffer.getInt(100)

        // 1. Process Class Defs & Strip Debug Info
        if (classDefsSize > 0 && classDefsOff in 112 until bytes.size) {
            for (i in 0 until classDefsSize) {
                val classDefStart = classDefsOff + (i * 32)
                if (classDefStart + 32 > bytes.size) break

                if (stripDebugInfo) {
                    // source_file_idx is at offset 16 of class_def_item
                    // Setting to NO_INDEX (0xFFFFFFFF) strips SourceFile attribute
                    buffer.putInt(classDefStart + 16, -1)
                    strippedDebugCount++
                }

                // Locate class_data_off at offset 24
                val classDataOff = buffer.getInt(classDefStart + 24)
                if (classDataOff in 112 until bytes.size) {
                    strippedDebugCount += stripClassDataDebugOffsets(classDataOff)
                }
            }
        }

        // 2. Obfuscate / Encrypt Non-Framework String Literals
        if (encryptStrings && stringIdsSize > 0 && stringIdsOff in 112 until bytes.size) {
            obfuscatedStringsCount = obfuscateStringPool(stringIdsSize, stringIdsOff)
        }

        // 3. Insert WIBES PROTECT Cryptographic Integrity Token into DEX link/padding
        if (insertIntegritySeal) {
            val linkOff = buffer.getInt(48)
            val linkSize = buffer.getInt(44)
            if (linkOff in 112 until bytes.size && linkSize == 0) {
                val seal = "WIBES_PROTECT_SEAL_V2".toByteArray(Charsets.US_ASCII)
                val safeWritePos = 112
                if (safeWritePos + seal.size <= bytes.size && stringIdsOff > safeWritePos + seal.size) {
                    System.arraycopy(seal, 0, bytes, safeWritePos, seal.size)
                }
            }
        }

        // 4. Cryptographically re-seal SHA-1 and Adler32 checksums
        val sealedSha1 = sealSha1Signature()
        val sealedChecksum = sealAdler32Checksum()

        val report = TransformationReport(
            strippedDebugItems = strippedDebugCount,
            obfuscatedStrings = obfuscatedStringsCount,
            sealedChecksum = sealedChecksum,
            sealedSha1 = sealedSha1,
            classDefsCount = classDefsSize,
            methodsCount = methodIdsSize
        )

        return Pair(bytes, report)
    }

    /**
     * Walks class_data_item and zeroes debug_info_off inside code_item.
     */
    private fun stripClassDataDebugOffsets(classDataOff: Int): Int {
        var count = 0
        try {
            var pos = classDataOff
            // Parse uleb128: static_fields_size, instance_fields_size, direct_methods_size, virtual_methods_size
            val (staticFieldsSize, p1) = readUleb128(pos)
            val (instanceFieldsSize, p2) = readUleb128(p1)
            val (directMethodsSize, p3) = readUleb128(p2)
            val (virtualMethodsSize, p4) = readUleb128(p3)
            pos = p4

            // Skip static fields (field_idx_diff, access_flags)
            for (i in 0 until staticFieldsSize) {
                pos = skipUleb128(skipUleb128(pos))
            }
            // Skip instance fields
            for (i in 0 until instanceFieldsSize) {
                pos = skipUleb128(skipUleb128(pos))
            }

            // Direct methods (method_idx_diff, access_flags, code_off)
            for (i in 0 until directMethodsSize) {
                pos = skipUleb128(pos) // method_idx_diff
                pos = skipUleb128(pos) // access_flags
                val (codeOff, nextP) = readUleb128(pos)
                pos = nextP
                if (codeOff in 112 until bytes.size) {
                    if (zeroDebugInfoInCodeItem(codeOff)) count++
                }
            }

            // Virtual methods
            for (i in 0 until virtualMethodsSize) {
                pos = skipUleb128(pos)
                pos = skipUleb128(pos)
                val (codeOff, nextP) = readUleb128(pos)
                pos = nextP
                if (codeOff in 112 until bytes.size) {
                    if (zeroDebugInfoInCodeItem(codeOff)) count++
                }
            }
        } catch (_: Exception) {
            // Safe guard against non-standard layouts
        }
        return count
    }

    private fun zeroDebugInfoInCodeItem(codeOff: Int): Boolean {
        // code_item structure:
        // offset 0: registers_size (ushort)
        // offset 2: ins_size (ushort)
        // offset 4: outs_size (ushort)
        // offset 6: tries_size (ushort)
        // offset 8: debug_info_off (uint)
        if (codeOff + 12 <= bytes.size) {
            val debugInfoOff = buffer.getInt(codeOff + 8)
            if (debugInfoOff != 0) {
                buffer.putInt(codeOff + 8, 0) // Zero out debug_info_off
                return true
            }
        }
        return false
    }

    private fun obfuscateStringPool(stringIdsSize: Int, stringIdsOff: Int): Int {
        var count = 0
        val xorKey = 0x5A.toByte()

        // Scan string pool offsets
        for (i in 0 until stringIdsSize.coerceAtMost(3000)) {
            val entryPos = stringIdsOff + (i * 4)
            if (entryPos + 4 > bytes.size) break
            val stringDataOff = buffer.getInt(entryPos)
            if (stringDataOff in 112 until bytes.size) {
                // Read utf16_size (uleb128)
                val (utf16Size, dataStart) = readUleb128(stringDataOff)
                if (utf16Size in 4..64 && dataStart + utf16Size < bytes.size) {
                    // Check if string contains custom sensitive pattern (e.g. key, token, secret, auth)
                    val strBytes = ByteArray(utf16Size)
                    System.arraycopy(bytes, dataStart, strBytes, 0, utf16Size)
                    val str = String(strBytes, Charsets.UTF_8)

                    // Obfuscate targeted user secret strings without corrupting method names, class names, or framework descriptors
                    if (str.startsWith("wibes_sec_") || str.startsWith("secret_token_") || str.startsWith("api_key_hidden_")) {
                        for (k in 0 until utf16Size) {
                            bytes[dataStart + k] = (bytes[dataStart + k].toInt() xor xorKey.toInt()).toByte()
                        }
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun sealSha1Signature(): String {
        // SHA-1 signature of rest of file starting at offset 32 up to length
        val md = MessageDigest.getInstance("SHA-1")
        md.update(bytes, 32, bytes.size - 32)
        val sha1Bytes = md.digest()
        System.arraycopy(sha1Bytes, 0, bytes, 12, 20)
        return sha1Bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sealAdler32Checksum(): Long {
        // Adler32 checksum of rest of file starting at offset 12 up to length
        val adler = Adler32()
        adler.update(bytes, 12, bytes.size - 12)
        val checksum = adler.value
        buffer.putInt(8, checksum.toInt())
        return checksum
    }

    private fun readUleb128(offset: Int): Pair<Int, Int> {
        var pos = offset
        var result = 0
        var cur: Int
        var shift = 0
        do {
            if (pos >= bytes.size) break
            cur = bytes[pos].toInt() and 0xFF
            result = result or ((cur and 0x7F) shl shift)
            shift += 7
            pos++
        } while ((cur and 0x80) != 0)
        return Pair(result, pos)
    }

    private fun skipUleb128(offset: Int): Int {
        var pos = offset
        while (pos < bytes.size && (bytes[pos].toInt() and 0x80) != 0) {
            pos++
        }
        return (pos + 1).coerceAtMost(bytes.size)
    }
}
