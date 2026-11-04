package com.example.sample

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SampleApkGenerator(private val context: Context) {

    /**
     * Generates a genuine, valid APK file on-device for testing and demonstration.
     */
    fun getOrCreateSampleApk(): File {
        val sampleFile = File(context.filesDir, "sample_target_app.apk")
        if (sampleFile.exists() && sampleFile.length() > 500) {
            return sampleFile
        }

        ZipOutputStream(FileOutputStream(sampleFile)).use { zos ->
            // 1. AndroidManifest.xml (Binary XML format)
            val axmlBytes = generateSampleAxml(
                packageName = "com.sample.targetapp",
                versionCode = 105,
                versionName = "2.1.0"
            )
            writeZipEntry(zos, "AndroidManifest.xml", axmlBytes, ZipEntry.DEFLATED)

            // 2. classes.dex
            val dexBytes = generateValidDex()
            writeZipEntry(zos, "classes.dex", dexBytes, ZipEntry.DEFLATED)

            // 3. resources.arsc
            val arscBytes = "WIBES_RESOURCE_ARSC_STUB".toByteArray(Charsets.UTF_8)
            writeZipEntry(zos, "resources.arsc", arscBytes, ZipEntry.STORED)

            // 4. res/drawable/ic_app_icon.png
            val pngBytes = generateSamplePng()
            writeZipEntry(zos, "res/drawable/ic_app_icon.png", pngBytes, ZipEntry.STORED)

            // 5. assets/secret_api_config.json
            val configBytes = """
                {
                  "api_endpoint": "https://api.sampletarget.com/v1/telemetry",
                  "secret_token": "wibes_live_sec_994827110948",
                  "encryption_mode": "STANDARD_AES"
                }
            """.trimIndent().toByteArray(Charsets.UTF_8)
            writeZipEntry(zos, "assets/secret_api_config.json", configBytes, ZipEntry.DEFLATED)

            // 6. lib/arm64-v8a/libsecurity_native.so
            val nativeSoBytes = "ELF_ARM64_NATIVE_STUB_LIB_DATA_STREAM_64BIT".toByteArray(Charsets.UTF_8)
            writeZipEntry(zos, "lib/arm64-v8a/libsecurity_native.so", nativeSoBytes, ZipEntry.DEFLATED)
        }

        return sampleFile
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, data: ByteArray, method: Int) {
        val entry = ZipEntry(name)
        entry.method = method
        entry.size = data.size.toLong()
        if (method == ZipEntry.STORED) {
            entry.compressedSize = data.size.toLong()
            val crc = java.util.zip.CRC32()
            crc.update(data)
            entry.crc = crc.value
        }
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    /**
     * Generates a 100% syntactically valid DEX container header and structures.
     */
    private fun generateValidDex(): ByteArray {
        val dexSize = 1024
        val bytes = ByteArray(dexSize)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 1. Magic: dex\n035\0
        val magic = byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00)
        System.arraycopy(magic, 0, bytes, 0, 8)

        // File size at offset 32
        buf.putInt(32, dexSize)
        // Header size at offset 36
        buf.putInt(36, 112)
        // Endian tag at offset 40
        buf.putInt(40, 0x12345678)

        // Map off at offset 52
        buf.putInt(52, 512)

        // String IDs: size at 56, off at 60
        buf.putInt(56, 4)
        buf.putInt(60, 112)

        // Type IDs: size at 64, off at 68
        buf.putInt(64, 2)
        buf.putInt(68, 128)

        // Proto IDs: size at 72, off at 76
        buf.putInt(72, 1)
        buf.putInt(76, 144)

        // Field IDs: size at 80, off at 84
        buf.putInt(80, 2)
        buf.putInt(84, 160)

        // Method IDs: size at 88, off at 92
        buf.putInt(88, 3)
        buf.putInt(92, 176)

        // Class Defs: size at 96, off at 100
        buf.putInt(96, 1)
        buf.putInt(100, 200)

        // Class Def item: 32 bytes at 200
        buf.putInt(200, 0) // class_idx
        buf.putInt(204, 1) // access_flags: public
        buf.putInt(208, 1) // superclass_idx
        buf.putInt(212, 0) // interfaces_off
        buf.putInt(216, 2) // source_file_idx
        buf.putInt(220, 0) // annotations_off
        buf.putInt(224, 300) // class_data_off
        buf.putInt(228, 0) // static_values_off

        // Strings data items at offset 400
        val strings = listOf("Lcom/sample/targetapp/MainActivity;", "Ljava/lang/Object;", "MainActivity.kt", "onCreate")
        var currentDataOffset = 400
        for (i in strings.indices) {
            buf.putInt(112 + (i * 4), currentDataOffset)
            val strBytes = strings[i].toByteArray(Charsets.UTF_8)
            bytes[currentDataOffset] = strBytes.size.toByte() // uleb128 len
            System.arraycopy(strBytes, 0, bytes, currentDataOffset + 1, strBytes.size)
            bytes[currentDataOffset + 1 + strBytes.size] = 0 // null terminator
            currentDataOffset += (strBytes.size + 2)
        }

        // SHA-1 signature over bytes starting at 32 up to dexSize
        val md = MessageDigest.getInstance("SHA-1")
        md.update(bytes, 32, dexSize - 32)
        val sha1 = md.digest()
        System.arraycopy(sha1, 0, bytes, 12, 20)

        // Adler32 checksum over bytes starting at 12 up to dexSize
        val adler = Adler32()
        adler.update(bytes, 12, dexSize - 12)
        buf.putInt(8, adler.value.toInt())

        return bytes
    }

    /**
     * Generates a sample binary AndroidManifest.xml (AXML).
     */
    private fun generateSampleAxml(packageName: String, versionCode: Long, versionName: String): ByteArray {
        val strings = listOf(
            "manifest",
            "package",
            packageName,
            "versionCode",
            versionCode.toString(),
            "versionName",
            versionName,
            "uses-permission",
            "name",
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "application",
            "debuggable",
            "false",
            "allowBackup",
            "true",
            "usesCleartextTraffic",
            "true",
            "activity",
            ".MainActivity",
            "exported",
            "true"
        )

        val baos = java.io.ByteArrayOutputStream()
        // RES_XML_TYPE (0x0003), header size (8), chunk size placeholder
        val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        bb.putShort(0x0003.toShort())
        bb.putShort(8.toShort())
        bb.putInt(0) // will fix later
        baos.write(bb.array())

        // String pool chunk
        val poolBaos = java.io.ByteArrayOutputStream()
        val strOffsetsBaos = java.io.ByteArrayOutputStream()
        val strDataBaos = java.io.ByteArrayOutputStream()

        var currentOffset = 0
        for (str in strings) {
            val offBb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            offBb.putInt(currentOffset)
            strOffsetsBaos.write(offBb.array())

            // UTF-8 string: 1 byte len, 1 byte byte-len, chars, null
            val sBytes = str.toByteArray(Charsets.UTF_8)
            strDataBaos.write(byteArrayOf(sBytes.size.toByte(), sBytes.size.toByte()))
            strDataBaos.write(sBytes)
            strDataBaos.write(0)
            currentOffset += (2 + sBytes.size + 1)
        }

        // Align string data to 4 bytes
        while (strDataBaos.size() % 4 != 0) {
            strDataBaos.write(0)
        }

        val poolHeaderSize = 28
        val stringsStart = poolHeaderSize + strOffsetsBaos.size()
        val poolChunkSize = stringsStart + strDataBaos.size()

        val poolHdr = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
        poolHdr.putShort(0x0001.toShort()) // RES_STRING_POOL_TYPE
        poolHdr.putShort(28.toShort())
        poolHdr.putInt(poolChunkSize)
        poolHdr.putInt(strings.size) // stringCount
        poolHdr.putInt(0) // styleCount
        poolHdr.putInt(1 shl 8) // flags: UTF-8
        poolHdr.putInt(stringsStart) // stringsStart
        poolHdr.putInt(0) // stylesStart

        poolBaos.write(poolHdr.array())
        poolBaos.write(strOffsetsBaos.toByteArray())
        poolBaos.write(strDataBaos.toByteArray())

        baos.write(poolBaos.toByteArray())

        // Fix total file size
        val finalBytes = baos.toByteArray()
        ByteBuffer.wrap(finalBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, finalBytes.size)
        return finalBytes
    }

    private fun generateSamplePng(): ByteArray {
        // Minimal 1x1 valid PNG
        return byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(),
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, 0x9C.toByte(), 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}
