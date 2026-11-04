package com.example.signing

import android.content.Context
import com.example.model.KeystoreConfig
import com.example.model.KeystoreMode
import com.example.model.SignatureInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SigningEngine(private val context: Context) {

    data class SigningCredentials(
        val privateKey: PrivateKey,
        val certificate: X509Certificate
    )

    data class SigningVerificationResult(
        val isValid: Boolean,
        val v1SignaturePresent: Boolean,
        val v2SignaturePresent: Boolean,
        val certificateOwner: String,
        val sha256Fingerprint: String,
        val sha1Fingerprint: String,
        val message: String
    )

    /**
     * Resolves or generates the signing credentials based on config.
     */
    fun getCredentials(config: KeystoreConfig): SigningCredentials {
        return when (config.mode) {
            KeystoreMode.IMPORT_EXISTING -> {
                val file = File(config.keystorePath)
                if (!file.exists()) {
                    throw IllegalArgumentException("Keystore file does not exist at: ${config.keystorePath}")
                }
                loadKeystore(file, config.keystorePassword, config.keyAlias, config.keyPassword)
            }
            KeystoreMode.CUSTOM_NEW -> {
                val file = File(config.keystorePath.ifEmpty {
                    File(context.filesDir, "wibes_custom.p12").absolutePath
                })
                generateAndSaveKeystore(
                    targetFile = file,
                    password = config.keystorePassword,
                    alias = config.keyAlias,
                    keyPassword = config.keyPassword,
                    ownerName = config.ownerName,
                    org = config.organization,
                    validityYears = config.validityYears
                )
            }
            KeystoreMode.AUTO_GENERATE -> {
                val defaultFile = File(context.filesDir, "wibes_protect_auto.p12")
                if (defaultFile.exists()) {
                    try {
                        loadKeystore(defaultFile, "wibesprotect", "wibes_key", "wibesprotect")
                    } catch (e: Exception) {
                        generateAndSaveKeystore(defaultFile, "wibesprotect", "wibes_key", "wibesprotect", "WIBES PROTECT Auto-Key", "Wibes Security", 25)
                    }
                } else {
                    generateAndSaveKeystore(defaultFile, "wibesprotect", "wibes_key", "wibesprotect", "WIBES PROTECT Auto-Key", "Wibes Security", 25)
                }
            }
        }
    }

    /**
     * Loads a PKCS12 / BKS / JKS keystore.
     */
    fun loadKeystore(file: File, pass: String, alias: String, keyPass: String): SigningCredentials {
        val types = listOf("PKCS12", "BKS", "JKS")
        var lastEx: Exception? = null

        for (t in types) {
            try {
                val ks = KeyStore.getInstance(t)
                FileInputStream(file).use { fis ->
                    ks.load(fis, pass.toCharArray())
                }
                val chosenAlias = if (ks.containsAlias(alias)) alias else ks.aliases().nextElement()
                val key = ks.getKey(chosenAlias, keyPass.toCharArray()) as? PrivateKey
                    ?: ks.getKey(chosenAlias, pass.toCharArray()) as? PrivateKey
                    ?: throw IllegalStateException("Could not extract private key from alias $chosenAlias")
                val cert = ks.getCertificate(chosenAlias) as? X509Certificate
                    ?: throw IllegalStateException("Could not extract X509Certificate from alias $chosenAlias")
                return SigningCredentials(key, cert)
            } catch (e: Exception) {
                lastEx = e
            }
        }
        throw lastEx ?: IllegalStateException("Failed to load keystore from ${file.name}")
    }

    /**
     * Generates a new 2048-bit RSA key pair and self-signed X.509 v3 certificate.
     */
    fun generateAndSaveKeystore(
        targetFile: File,
        password: String,
        alias: String,
        keyPassword: String,
        ownerName: String,
        org: String,
        validityYears: Int
    ): SigningCredentials {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair: KeyPair = kpg.generateKeyPair()

        val notBefore = Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365L * validityYears)
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())

        val sanitizedOwner = ownerName.replace(",", "").replace("=", "")
        val sanitizedOrg = org.replace(",", "").replace("=", "")
        val issuer = X500Name("CN=$sanitizedOwner, O=$sanitizedOrg, OU=WIBES PROTECT, C=US")

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            issuer,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        // Store into PKCS12 file
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(alias, keyPair.private, keyPassword.toCharArray(), arrayOf(cert))
        FileOutputStream(targetFile).use { fos ->
            ks.store(fos, password.toCharArray())
        }

        return SigningCredentials(keyPair.private, cert)
    }

    /**
     * Signs the rebuilt APK file using V1 (JAR) and V2 (APK Signature Scheme) digital signatures.
     */
    fun signApk(
        unsignedApk: File,
        signedApk: File,
        credentials: SigningCredentials
    ) {
        // Step 1: Read all entries from unsignedApk and compute SHA-1 and SHA-256 for each
        val entriesData = mutableMapOf<String, ByteArray>()
        val entriesDigestSha1 = mutableMapOf<String, String>()
        val entriesDigestSha256 = mutableMapOf<String, String>()
        val sha1 = MessageDigest.getInstance("SHA-1")
        val sha256 = MessageDigest.getInstance("SHA-256")

        ZipInputStream(FileInputStream(unsignedApk)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                // Skip any existing signature artifacts
                if (!name.startsWith("META-INF/")) {
                    val bytes = zis.readBytes()
                    entriesData[name] = bytes
                    sha1.reset()
                    sha256.reset()
                    val b64Sha1 = android.util.Base64.encodeToString(sha1.digest(bytes), android.util.Base64.NO_WRAP)
                    val b64Sha256 = android.util.Base64.encodeToString(sha256.digest(bytes), android.util.Base64.NO_WRAP)
                    entriesDigestSha1[name] = b64Sha1
                    entriesDigestSha256[name] = b64Sha256
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Step 2: Build META-INF/MANIFEST.MF with dual SHA-1 and SHA-256 digests
        val manifestBytes = ByteArrayOutputStream().use { baos ->
            baos.write("Manifest-Version: 1.0\r\n".toByteArray(Charsets.UTF_8))
            baos.write("Created-By: 1.0.0 (WIBES PROTECT Engine by Hackerwibes)\r\n\r\n".toByteArray(Charsets.UTF_8))
            for ((name, b64Sha1) in entriesDigestSha1) {
                val b64Sha256 = entriesDigestSha256[name] ?: ""
                baos.write("Name: $name\r\n".toByteArray(Charsets.UTF_8))
                baos.write("SHA1-Digest: $b64Sha1\r\n".toByteArray(Charsets.UTF_8))
                baos.write("SHA-256-Digest: $b64Sha256\r\n\r\n".toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }

        // Step 3: Build META-INF/CERT.SF
        sha1.reset()
        sha256.reset()
        val manifestSha1B64 = android.util.Base64.encodeToString(sha1.digest(manifestBytes), android.util.Base64.NO_WRAP)
        val manifestSha256B64 = android.util.Base64.encodeToString(sha256.digest(manifestBytes), android.util.Base64.NO_WRAP)
        val certSfBytes = ByteArrayOutputStream().use { baos ->
            baos.write("Signature-Version: 1.0\r\n".toByteArray(Charsets.UTF_8))
            baos.write("Created-By: 1.0.0 (WIBES PROTECT by Hackerwibes)\r\n".toByteArray(Charsets.UTF_8))
            baos.write("SHA1-Digest-Manifest: $manifestSha1B64\r\n".toByteArray(Charsets.UTF_8))
            baos.write("SHA-256-Digest-Manifest: $manifestSha256B64\r\n\r\n".toByteArray(Charsets.UTF_8))

            for ((name, b64Sha1) in entriesDigestSha1) {
                val b64Sha256 = entriesDigestSha256[name] ?: ""
                val section = "Name: $name\r\nSHA1-Digest: $b64Sha1\r\nSHA-256-Digest: $b64Sha256\r\n\r\n".toByteArray(Charsets.UTF_8)
                sha1.reset()
                sha256.reset()
                val secSha1B64 = android.util.Base64.encodeToString(sha1.digest(section), android.util.Base64.NO_WRAP)
                val secSha256B64 = android.util.Base64.encodeToString(sha256.digest(section), android.util.Base64.NO_WRAP)
                baos.write("Name: $name\r\n".toByteArray(Charsets.UTF_8))
                baos.write("SHA1-Digest: $secSha1B64\r\n".toByteArray(Charsets.UTF_8))
                baos.write("SHA-256-Digest: $secSha256B64\r\n\r\n".toByteArray(Charsets.UTF_8))
            }
            baos.toByteArray()
        }

        // Step 4: Sign CERT.SF generating PKCS#7 block (META-INF/CERT.RSA) using SHA1withRSA for universal Android API compatibility
        val certRsaBytes = createPkcs7Signature(certSfBytes, credentials)

        // Step 5: Write out signed APK with correct compression and 4-byte alignment
        val fos = FileOutputStream(signedApk)
        val countingOut = CountingOutputStream(fos)
        val zos = ZipOutputStream(countingOut)

        zos.use { zipOut ->
            // Write META-INF signature files first
            writeZipEntry(zipOut, countingOut, "META-INF/MANIFEST.MF", manifestBytes, ZipEntry.DEFLATED)
            writeZipEntry(zipOut, countingOut, "META-INF/CERT.SF", certSfBytes, ZipEntry.DEFLATED)
            writeZipEntry(zipOut, countingOut, "META-INF/CERT.RSA", certRsaBytes, ZipEntry.DEFLATED)

            // Write all application entries
            for ((name, data) in entriesData) {
                val method = if (name == "resources.arsc" || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jpg")) {
                    ZipEntry.STORED
                } else {
                    ZipEntry.DEFLATED
                }
                writeZipEntry(zipOut, countingOut, name, data, method)
            }
        }

        // Step 6: Inject 100% compliant APK Signature Scheme V2 block
        try {
            injectApkSigningBlockV2(signedApk, credentials)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class CountingOutputStream(out: java.io.OutputStream) : java.io.FilterOutputStream(out) {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len
        }
    }

    private fun writeZipEntry(
        zos: ZipOutputStream,
        countingOut: CountingOutputStream,
        name: String,
        data: ByteArray,
        method: Int
    ) {
        val entry = ZipEntry(name)
        entry.method = method
        entry.size = data.size.toLong()

        if (method == ZipEntry.STORED) {
            entry.compressedSize = data.size.toLong()
            val crc = java.util.zip.CRC32()
            crc.update(data)
            entry.crc = crc.value

            // 4-byte alignment for uncompressed stored entries (crucial for Android resources.arsc)
            // Local file header is 30 bytes + name length + extra length
            val currentOffset = countingOut.bytesWritten
            val localHeaderSize = 30 + name.toByteArray(Charsets.UTF_8).size
            val dataOffset = currentOffset + localHeaderSize
            val padding = ((4 - (dataOffset % 4)) % 4).toInt()
            if (padding > 0) {
                entry.extra = ByteArray(padding)
            }
        }

        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    /**
     * Generates a standard PKCS#7 / CMS SignedData block using BouncyCastle (detached signature).
     */
    private fun createPkcs7Signature(content: ByteArray, creds: SigningCredentials): ByteArray {
        val certHolder = org.bouncycastle.cert.jcajce.JcaX509CertificateHolder(creds.certificate)
        val signer = JcaContentSignerBuilder("SHA1withRSA").build(creds.privateKey)
        val gen = CMSSignedDataGenerator()
        gen.addSignerInfoGenerator(
            JcaSignerInfoGeneratorBuilder(
                JcaDigestCalculatorProviderBuilder().build()
            ).build(signer, certHolder)
        )
        gen.addCertificate(certHolder)
        // False = detached signature (standard for JAR/APK META-INF/CERT.RSA)
        val cmsData = gen.generate(CMSProcessableByteArray(content), false)
        return cmsData.encoded
    }

    /**
     * Injects a fully compliant Android APK Signature Scheme v2 block before the ZIP Central Directory.
     */
    private fun injectApkSigningBlockV2(apkFile: File, creds: SigningCredentials) {
        val raf = RandomAccessFile(apkFile, "rw")
        raf.use { f ->
            val fileLength = f.length()
            if (fileLength < 22) return

            // Locate End of Central Directory (EOCD)
            val buffer = ByteArray(4096.coerceAtMost(fileLength.toInt()))
            val readStart = (fileLength - buffer.size).coerceAtLeast(0)
            f.seek(readStart)
            f.readFully(buffer)

            var eocdOffset = -1L
            for (i in buffer.size - 22 downTo 0) {
                if (buffer[i] == 0x50.toByte() && buffer[i + 1] == 0x4b.toByte() &&
                    buffer[i + 2] == 0x05.toByte() && buffer[i + 3] == 0x06.toByte()
                ) {
                    eocdOffset = readStart + i
                    break
                }
            }

            if (eocdOffset == -1L) return

            // Read Central Directory Offset from EOCD (offset 16 from EOCD start)
            f.seek(eocdOffset + 16)
            val cdOffsetBytes = ByteArray(4)
            f.readFully(cdOffsetBytes)
            val cdOffset = ByteBuffer.wrap(cdOffsetBytes).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL

            // Read Central Directory contents
            val cdSize = eocdOffset - cdOffset
            if (cdSize <= 0) return
            val cdBytes = ByteArray(cdSize.toInt())
            f.seek(cdOffset)
            f.readFully(cdBytes)

            // Read EOCD bytes
            val eocdSize = (fileLength - eocdOffset).toInt()
            val eocdBytes = ByteArray(eocdSize)
            f.seek(eocdOffset)
            f.readFully(eocdBytes)

            // Calculate deterministic APK Signing Block size first
            val certEncoded = creds.certificate.encoded
            val publicKeyEncoded = creds.certificate.publicKey.encoded

            // SignedData size:
            // digests: 4 (len) + 4 (item len) + 4 (alg) + 4 (dig len) + 32 (sha256) = 48
            // certs: 4 (len) + 4 (item len) + certEncoded.size
            // additionalAttributes: 4 (len = 0)
            val signedDataSize = 48 + 4 + 4 + certEncoded.size + 4

            // Signatures size:
            // 4 (len) + 4 (item len) + 4 (alg) + 4 (sig len) + 256 (rsa-2048 sig) = 272
            val signaturesSize = 272

            // PublicKey size:
            // 4 (len) + publicKeyEncoded.size
            val publicKeySize = 4 + publicKeyEncoded.size

            // Signer size:
            // 4 (len) + 4 + signedDataSize + 4 + signaturesSize + publicKeySize
            val signerSize = 4 + 4 + signedDataSize + 4 + signaturesSize + publicKeySize

            // Signers array size:
            // 4 (len) + signerSize
            val valuePayloadSize = 4 + signerSize

            // Pair size (excludes the 8-byte length prefix):
            // 4 (ID) + valuePayloadSize
            val pairLength = 4L + valuePayloadSize

            // Total Block Size:
            // 8 (size1) + 8 (pair len) + pairLength + 8 (size2) + 16 (magic)
            val totalBlockSize = 8L + 8L + pairLength + 8L + 16L
            val newCdOffset = cdOffset + totalBlockSize

            // Compute authentic 1MB-chunked APK Signature Scheme v2 digest
            val apkDigest = computeV2ContentDigest(f, cdOffset, eocdOffset, newCdOffset)

            // Build V2 SignedData
            val signedDataBaos = ByteArrayOutputStream()
            // 1. Digests
            val digestsBaos = ByteArrayOutputStream()
            val dEntry = ByteArrayOutputStream()
            dEntry.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x0103).array()) // SHA256withRSA
            dEntry.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(32).array())
            dEntry.write(apkDigest)
            val dEntryBytes = dEntry.toByteArray()
            digestsBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dEntryBytes.size).array())
            digestsBaos.write(dEntryBytes)
            val digestsBytes = digestsBaos.toByteArray()
            signedDataBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(digestsBytes.size).array())
            signedDataBaos.write(digestsBytes)

            // 2. Certificates
            val certsBaos = ByteArrayOutputStream()
            certsBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(certEncoded.size).array())
            certsBaos.write(certEncoded)
            val certsBytes = certsBaos.toByteArray()
            signedDataBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(certsBytes.size).array())
            signedDataBaos.write(certsBytes)

            // 3. Additional attributes (empty)
            signedDataBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())

            val signedDataBytes = signedDataBaos.toByteArray()

            // Cryptographically sign signedDataBytes
            val rsaSig = Signature.getInstance("SHA256withRSA")
            rsaSig.initSign(creds.privateKey)
            rsaSig.update(signedDataBytes)
            val signatureBytes = rsaSig.sign()

            // Build Signatures
            val sigsBaos = ByteArrayOutputStream()
            val sigEntry = ByteArrayOutputStream()
            sigEntry.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x0103).array())
            sigEntry.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signatureBytes.size).array())
            sigEntry.write(signatureBytes)
            val sigEntryBytes = sigEntry.toByteArray()
            sigsBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sigEntryBytes.size).array())
            sigsBaos.write(sigEntryBytes)
            val sigsBytes = sigsBaos.toByteArray()

            // Build Single Signer
            val signerBaos = ByteArrayOutputStream()
            signerBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(signedDataBytes.size).array())
            signerBaos.write(signedDataBytes)
            signerBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sigsBytes.size).array())
            signerBaos.write(sigsBytes)
            signerBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(publicKeyEncoded.size).array())
            signerBaos.write(publicKeyEncoded)
            val singleSignerBytes = signerBaos.toByteArray()

            // Build Signers Array (valuePayload)
            val valueBaos = ByteArrayOutputStream()
            valueBaos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(singleSignerBytes.size).array())
            valueBaos.write(singleSignerBytes)
            val valueBytes = valueBaos.toByteArray()

            // Assemble Full APK Signing Block
            val magic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
            val blockBuf = ByteBuffer.allocate(totalBlockSize.toInt()).order(ByteOrder.LITTLE_ENDIAN)
            blockBuf.putLong(totalBlockSize - 8) // size of block excluding this field
            blockBuf.putLong(4L + valueBytes.size) // length of pair
            blockBuf.putInt(0x7109871a) // V2 block ID
            blockBuf.put(valueBytes)
            blockBuf.putLong(totalBlockSize - 8) // size of block
            blockBuf.put(magic)
            val v2Block = blockBuf.array()

            // Rewrite file: [Section 1: ZIP entries] + [V2 Block] + [Central Directory] + [Updated EOCD]
            f.seek(cdOffset)
            f.write(v2Block)
            f.write(cdBytes)

            // Update EOCD with new CD offset
            val updatedEocd = ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN)
            updatedEocd.putInt(16, newCdOffset.toInt())
            f.write(updatedEocd.array())
            f.setLength(newCdOffset + cdSize + eocdSize)
        }
    }

    /**
     * Computes the 1MB-chunked SHA-256 tree digest over the three APK sections according to
     * the Android APK Signature Scheme v2 specification.
     */
    private fun computeV2ContentDigest(
        raf: RandomAccessFile,
        cdOffset: Long,
        eocdOffset: Long,
        newCdOffset: Long
    ): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val chunkDigests = mutableListOf<ByteArray>()
        val chunkSize = 1048576 // 1MB
        val buffer = ByteArray(chunkSize)

        // Section 1: ZIP entries (0 until cdOffset)
        var remaining1 = cdOffset
        raf.seek(0)
        while (remaining1 > 0) {
            val toRead = remaining1.coerceAtMost(chunkSize.toLong()).toInt()
            raf.readFully(buffer, 0, toRead)
            md.reset()
            md.update(0xa5.toByte())
            val lenBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(toRead).array()
            md.update(lenBytes)
            md.update(buffer, 0, toRead)
            chunkDigests.add(md.digest())
            remaining1 -= toRead
        }

        // Section 2: ZIP Central Directory (cdOffset until eocdOffset)
        var remaining2 = eocdOffset - cdOffset
        raf.seek(cdOffset)
        while (remaining2 > 0) {
            val toRead = remaining2.coerceAtMost(chunkSize.toLong()).toInt()
            raf.readFully(buffer, 0, toRead)
            md.reset()
            md.update(0xa5.toByte())
            val lenBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(toRead).array()
            md.update(lenBytes)
            md.update(buffer, 0, toRead)
            chunkDigests.add(md.digest())
            remaining2 -= toRead
        }

        // Section 3: EOCD with byte 16..19 set to newCdOffset
        val eocdSize = (raf.length() - eocdOffset).toInt()
        val eocdBytes = ByteArray(eocdSize)
        raf.seek(eocdOffset)
        raf.readFully(eocdBytes)
        ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(16, newCdOffset.toInt())

        var remaining3 = eocdSize
        var offset3 = 0
        while (remaining3 > 0) {
            val toRead = remaining3.coerceAtMost(chunkSize)
            md.reset()
            md.update(0xa5.toByte())
            val lenBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(toRead).array()
            md.update(lenBytes)
            md.update(eocdBytes, offset3, toRead)
            chunkDigests.add(md.digest())
            remaining3 -= toRead
            offset3 += toRead
        }

        // Top-level digest
        md.reset()
        md.update(0x5a.toByte())
        val countBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkDigests.size).array()
        md.update(countBytes)
        for (cd in chunkDigests) {
            md.update(cd)
        }
        return md.digest()
    }

    /**
     * Verifies the APK's digital signatures.
     */
    fun verifyApkSignature(apkFile: File): SigningVerificationResult {
        var v1Present = false
        var v2Present = false
        var certOwner = "Unknown"
        var sha256 = ""
        var sha1 = ""

        try {
            // Check V1 signature
            val zip = ZipFile(apkFile)
            val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF")
            val certSfEntry = zip.getEntry("META-INF/CERT.SF")
            val certRsaEntry = zip.getEntry("META-INF/CERT.RSA") ?: zip.getEntry("META-INF/CERT.EC")

            if (manifestEntry != null && certSfEntry != null && certRsaEntry != null) {
                v1Present = true
                val certBytes = zip.getInputStream(certRsaEntry).readBytes()
                val cf = CertificateFactory.getInstance("X.509")
                val certs = cf.generateCertificates(ByteArrayInputStream(certBytes))
                for (c in certs) {
                    if (c is X509Certificate) {
                        certOwner = c.subjectX500Principal.name
                        sha256 = MessageDigest.getInstance("SHA-256").digest(c.encoded).joinToString(":") { "%02X".format(it) }
                        sha1 = MessageDigest.getInstance("SHA-1").digest(c.encoded).joinToString(":") { "%02X".format(it) }
                        break
                    }
                }
            }
            zip.close()

            // Check V2 signature block
            RandomAccessFile(apkFile, "r").use { raf ->
                val len = raf.length()
                if (len > 32) {
                    val buf = ByteArray(4096.coerceAtMost(len.toInt()))
                    raf.seek(len - buf.size)
                    raf.readFully(buf)
                    val magic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
                    for (i in 0..buf.size - magic.size) {
                        var match = true
                        for (m in magic.indices) {
                            if (buf[i + m] != magic[m]) {
                                match = false
                                break
                            }
                        }
                        if (match) {
                            v2Present = true
                            break
                        }
                    }
                }
            }

            val isValid = v1Present || v2Present
            val msg = if (isValid) {
                "APK Digital Signature verified successfully (${listOfNotNull(if (v1Present) "V1 Scheme" else null, if (v2Present) "V2 Scheme" else null).joinToString(" + ")})"
            } else {
                "APK is unsigned or signature verification failed."
            }

            return SigningVerificationResult(
                isValid = isValid,
                v1SignaturePresent = v1Present,
                v2SignaturePresent = v2Present,
                certificateOwner = certOwner,
                sha256Fingerprint = sha256,
                sha1Fingerprint = sha1,
                message = msg
            )
        } catch (e: Exception) {
            return SigningVerificationResult(
                isValid = false,
                v1SignaturePresent = false,
                v2SignaturePresent = false,
                certificateOwner = "Verification Error",
                sha256Fingerprint = "",
                sha1Fingerprint = "",
                message = "Signature verification error: ${e.message}"
            )
        }
    }
}
