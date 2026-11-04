package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ApkInfo
import com.example.model.ProtectionProfile
import com.example.model.ProtectionProgress
import com.example.ui.MainUiState
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberGradientButton
import com.example.ui.components.CyberPill
import com.example.ui.components.LogTerminalCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryPurple
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onBrowseApk: () -> Unit,
    onLoadDemoApk: () -> Unit,
    onSelectProfile: (ProtectionProfile) -> Unit,
    onStartProtection: () -> Unit,
    onClearLogs: () -> Unit,
    onOpenAboutApp: () -> Unit,
    onOpenAboutAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToModules: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Professional Shield Logo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryPurple, SecondaryPurple, ElectricViolet)
                            )
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_wibes_shield),
                        contentDescription = "WIBES Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WIBES",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PROTECT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = ElectricViolet
                        )
                    }
                    Text(
                        text = "BEYOND OBFUSCATION. BUILT FOR PROTECTION.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Three-dot overflow menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("overflow_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("About App") },
                        onClick = {
                            menuExpanded = false
                            onOpenAboutApp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("About Admin") },
                        onClick = {
                            menuExpanded = false
                            onOpenAboutAdmin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // APK INFORMATION & STATUS CARD
        ApkInformationCard(
            apkInfo = uiState.selectedApkInfo,
            isAnalyzing = uiState.isAnalyzing,
            onInspect = onNavigateToAnalyzer
        )

        Spacer(modifier = Modifier.height(14.dp))

        // APK SELECTOR CARD
        ApkSelectorCard(
            selectedFileName = uiState.selectedApkInfo?.fileName,
            fileSize = uiState.selectedApkInfo?.fileSize,
            onBrowse = onBrowseApk,
            onLoadDemo = onLoadDemoApk
        )

        Spacer(modifier = Modifier.height(14.dp))

        // PROTECTION PROFILES SELECTOR
        ProfileSelectorCard(
            currentProfile = uiState.currentProfile,
            activeModulesCount = uiState.modules.count { it.isEnabled },
            totalModulesCount = uiState.modules.size,
            onSelectProfile = onSelectProfile,
            onConfigureModules = onNavigateToModules
        )

        Spacer(modifier = Modifier.height(18.dp))

        // LARGE PREMIUM GRADIENT BUTTON: PROTECT APK
        CyberGradientButton(
            text = if (uiState.progress.isRunning) "PROTECTING APK..." else "PROTECT APK",
            icon = if (uiState.progress.isRunning) Icons.Default.Sync else Icons.Default.Shield,
            enabled = uiState.selectedApkInfo != null && !uiState.progress.isRunning,
            onClick = onStartProtection,
            testTag = "protect_apk_button"
        )

        // PROTECTION STATUS PROGRESS CARD (Visible during or after execution)
        AnimatedVisibility(visible = uiState.progress.isRunning || uiState.progress.isCompleted || uiState.progress.isFailed) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                ProtectionStatusCard(progress = uiState.progress)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REAL-TIME PROTECTION LOGS CARD
        LogTerminalCard(
            logs = uiState.logs,
            onClearLogs = onClearLogs
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ApkInformationCard(
    apkInfo: ApkInfo?,
    isAnalyzing: Boolean,
    onInspect: () -> Unit
) {
    CyberCard(glowColor = ElectricViolet.copy(alpha = 0.3f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APK INFORMATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = ElectricViolet
                )

                if (apkInfo != null) {
                    CyberPill(
                        text = "READY",
                        color = SuccessGreen
                    )
                } else if (isAnalyzing) {
                    CyberPill(
                        text = "ANALYZING...",
                        color = CyberCyan
                    )
                } else {
                    CyberPill(
                        text = "NO APK SELECTED",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAnalyzing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ElectricViolet,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing DEX bytecode and package structures...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (apkInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = ElectricVioletBright,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = apkInfo.fileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = apkInfo.packageName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text(
                                text = "v${apkInfo.versionName} (${apkInfo.versionCode}) • ${formatSize(apkInfo.fileSize)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onInspect,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Select an existing APK from storage or load the demo test package to analyze vulnerabilities and apply hardening layers.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ApkSelectorCard(
    selectedFileName: String?,
    fileSize: Long?,
    onBrowse: () -> Unit,
    onLoadDemo: () -> Unit
) {
    CyberCard(glowColor = PrimaryPurple.copy(alpha = 0.2f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SELECT APK FILE",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFileName ?: "No file chosen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (fileSize != null && fileSize > 0) formatSize(fileSize) else "Tap browse to select",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = onLoadDemo,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("load_demo_apk_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Demo APK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onBrowse,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("browse_apk_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ElectricViolet
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSelectorCard(
    currentProfile: ProtectionProfile,
    activeModulesCount: Int,
    totalModulesCount: Int,
    onSelectProfile: (ProtectionProfile) -> Unit,
    onConfigureModules: () -> Unit
) {
    CyberCard(glowColor = ElectricViolet.copy(alpha = 0.25f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROTECTION PROFILE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = ElectricViolet
                    )
                    Text(
                        text = "$activeModulesCount of $totalModulesCount Modules Active",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Configure",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    modifier = Modifier
                        .clickable { onConfigureModules() }
                        .padding(4.dp)
                        .testTag("configure_modules_link")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProtectionProfile.entries.forEach { profile ->
                    val isSelected = currentProfile == profile
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(PrimaryPurple, SecondaryPurple))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ElectricViolet else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectProfile(profile) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("profile_chip_${profile.name.lowercase()}")
                    ) {
                        Text(
                            text = profile.displayName.replace(" Protection", ""),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = currentProfile.description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProtectionStatusCard(progress: ProtectionProgress) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage / 100f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    CyberCard(
        glowColor = if (progress.isFailed) ErrorRed else if (progress.isCompleted) SuccessGreen else ElectricViolet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricViolet,
                            strokeWidth = 2.dp
                        )
                    } else if (progress.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress.isRunning) "Stage ${progress.step}/${progress.totalSteps}: ${progress.stepName}"
                        else if (progress.isCompleted) "Protection & Signing Complete"
                        else "Protection Failed: ${progress.errorMessage ?: ""}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isFailed) ErrorRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${progress.percentage.toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricViolet
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress.isFailed) ErrorRed else ElectricViolet,
                trackColor = Color(0xFF0D0E17),
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}
