package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WIBES PROTECT", appName)
  }

  @Test
  fun `test pipeline execution and output apk`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleGenerator = com.example.sample.SampleApkGenerator(context)
    val sampleApk = sampleGenerator.getOrCreateSampleApk()
    val apkUri = android.net.Uri.fromFile(sampleApk)
    val engine = com.example.engine.ProtectionEngine(context)
    val profile = com.example.model.ProtectionProfile.MAXIMUM
    val modules = com.example.engine.ModulesRegistry.getModulesForProfile(profile)
    val keystoreConfig = com.example.model.KeystoreConfig()

    val result = engine.executePipeline(
      apkUri = apkUri,
      profile = profile,
      modules = modules,
      keystoreConfig = keystoreConfig,
      onLog = { println(it.message) },
      onProgress = { println("Step ${it.step}: ${it.stepName}") }
    )
    println("Output APK created at: ${result.outputFilePath}")
    val outputFile = java.io.File(result.outputFilePath)
    org.junit.Assert.assertTrue(outputFile.exists())
    org.junit.Assert.assertTrue(outputFile.length() > 0)

    // Run Android SDK apksigner tool on the output APK!
    val proc = ProcessBuilder(
      "/opt/android/sdk/build-tools/36.0.0/apksigner",
      "verify",
      "-v",
      "--verbose",
      "--min-sdk-version",
      "24",
      outputFile.absolutePath
    ).redirectErrorStream(true).start()
    val output = proc.inputStream.bufferedReader().readText()
    val exitCode = proc.waitFor()
    println("=== APKSIGNER VERIFY RESULT (exitCode=$exitCode) ===")
    println(output)
    println("====================================================")
    org.junit.Assert.assertEquals("apksigner verification must succeed! Output: $output", 0, exitCode)
  }

  @Test
  fun `test custom keystore signing and apksigner`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleGenerator = com.example.sample.SampleApkGenerator(context)
    val sampleApk = sampleGenerator.getOrCreateSampleApk()
    val apkUri = android.net.Uri.fromFile(sampleApk)
    val engine = com.example.engine.ProtectionEngine(context)
    val profile = com.example.model.ProtectionProfile.ADVANCED
    val modules = com.example.engine.ModulesRegistry.getModulesForProfile(profile)
    val customConfig = com.example.model.KeystoreConfig(
      mode = com.example.model.KeystoreMode.CUSTOM_NEW,
      keyAlias = "hackerwibes_custom",
      keystorePassword = "customPassword123",
      keyPassword = "customPassword123",
      ownerName = "Hackerwibes Custom Key",
      organization = "Wibes Custom Enterprise"
    )

    val result = engine.executePipeline(
      apkUri = apkUri,
      profile = profile,
      modules = modules,
      keystoreConfig = customConfig,
      onLog = { },
      onProgress = { }
    )
    val outputFile = java.io.File(result.outputFilePath)
    org.junit.Assert.assertTrue(outputFile.exists())

    val proc = ProcessBuilder(
      "/opt/android/sdk/build-tools/36.0.0/apksigner",
      "verify",
      "-v",
      "--min-sdk-version",
      "24",
      outputFile.absolutePath
    ).redirectErrorStream(true).start()
    val output = proc.inputStream.bufferedReader().readText()
    val exitCode = proc.waitFor()
    println("Custom Keystore apksigner output:\n$output")
    org.junit.Assert.assertEquals("Custom keystore apksigner must verify! Output: $output", 0, exitCode)
  }
}
