package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkInfo
import com.example.model.ComponentInfo
import com.example.model.DexInfo
import com.example.model.NativeLibInfo
import com.example.model.PermissionInfo
import com.example.model.SecurityAuditFinding
import com.example.model.Severity
import com.example.ui.components.CyberCard
import com.example.ui.components.SeverityBadge
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import java.util.Locale

@Composable
fun AnalyzerScreen(
    apkInfo: ApkInfo?,
    onLoadDemo: () -> Unit,
    onBrowse: () -> Unit
) {
    if (apkInfo == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ElectricViolet.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No APK Loaded For Analysis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select an existing APK from storage or load the built-in demo package to run deep bytecode and vulnerability inspection.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Security Audit", "Components", "DEX & Bytecode", "Native Libs", "Signatures")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .testTag("analyzer_screen")
    ) {
        // App Summary Banner
        CyberCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            glowColor = CyberCyan.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = apkInfo.packageName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v${apkInfo.versionName} (${apkInfo.versionCode}) • Min SDK ${apkInfo.minSdk} • Target SDK ${apkInfo.targetSdk}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val highCount = apkInfo.auditFindings.count { it.severity == Severity.HIGH }
                    if (highCount > 0) {
                        SeverityBadge(Severity.HIGH)
                    } else {
                        SeverityBadge(Severity.GOOD)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> AuditFindingsTab(apkInfo.auditFindings)
                1 -> ComponentsTab(apkInfo)
                2 -> DexTab(apkInfo.dexFiles)
                3 -> NativeLibsTab(apkInfo.nativeLibs)
                4 -> SignaturesTab(apkInfo)
            }
        }
    }
}

@Composable
private fun AuditFindingsTab(findings: List<SecurityAuditFinding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(findings) { finding ->
            CyberCard(
                glowColor = when (finding.severity) {
                    Severity.HIGH -> ErrorRed.copy(alpha = 0.4f)
                    Severity.MEDIUM -> WarningOrange.copy(alpha = 0.4f)
                    Severity.LOW -> CyberCyan.copy(alpha = 0.3f)
                    Severity.GOOD -> SuccessGreen.copy(alpha = 0.3f)
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = finding.category.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        SeverityBadge(finding.severity)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = finding.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = finding.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D0E17))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 ${finding.recommendation}",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ComponentsTab(apkInfo: ApkInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Manifest Permissions (${apkInfo.permissions.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet
            )
        }

        items(apkInfo.permissions) { perm ->
            CyberCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perm.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (perm.description.isNotEmpty()) {
                            Text(
                                text = perm.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (perm.isDangerous) {
                        SeverityBadge(Severity.HIGH)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registered Components",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet
            )
        }

        items(apkInfo.activities) { act ->
            ComponentRow("Activity", act)
        }

        items(apkInfo.services) { srv ->
            ComponentRow("Service", srv)
        }

        items(apkInfo.receivers) { rec ->
            ComponentRow("Receiver", rec)
        }

        items(apkInfo.providers) { prv ->
            ComponentRow("Provider", prv)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ComponentRow(type: String, comp: ComponentInfo) {
    CyberCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[$type] ${comp.simpleName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = comp.name,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (comp.isExported) {
                SeverityBadge(Severity.MEDIUM)
            }
        }
    }
}

@Composable
private fun DexTab(dexFiles: List<DexInfo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dexFiles) { dex ->
            CyberCard(glowColor = ElectricViolet.copy(alpha = 0.3f)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dex.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SeverityBadge(if (dex.checksumValid) Severity.GOOD else Severity.HIGH)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatPill("Classes", dex.classCount.toString())
                        StatPill("Methods", dex.methodCount.toString())
                        StatPill("Fields", dex.fieldCount.toString())
                        StatPill("Strings", dex.stringPoolSize.toString())
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SHA-1: ${dex.sha1Hex}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NativeLibsTab(libs: List<NativeLibInfo>) {
    if (libs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No native (.so) binaries detected in this APK archive.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(libs) { lib ->
            CyberCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lib.fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = lib.abi,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = "SHA-256: ${lib.sha256}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SignaturesTab(apkInfo: ApkInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CyberCard(glowColor = PrimaryPurple.copy(alpha = 0.3f)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Archive Hashes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricViolet
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SHA-256: ${apkInfo.sha256Hex}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MD5: ${apkInfo.md5Hex}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (apkInfo.signatures.isEmpty()) {
            item {
                CyberCard(glowColor = WarningOrange.copy(alpha = 0.3f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "No Embedded V1 Signature Certificate Found",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                        Text(
                            text = "The application will be digitally signed using WIBES PROTECT Signing Engine upon protection.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(apkInfo.signatures) { sig ->
                CyberCard(glowColor = SuccessGreen.copy(alpha = 0.3f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = sig.scheme,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Subject: ${sig.subject}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Algorithm: ${sig.sigAlg}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Validity: ${sig.validFrom} to ${sig.validTo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "SHA-256 Fingerprint:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = sig.sha256Fingerprint, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
