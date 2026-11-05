package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberCard
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    onOpenAboutApp: () -> Unit,
    onOpenAboutAdmin: () -> Unit,
    onClearHistory: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "SYSTEM SETTINGS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Configure engine preferences, diagnostics & security credits",
                fontSize = 12.sp,
                color = ElectricViolet
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            CyberCard(glowColor = ElectricViolet.copy(alpha = 0.2f)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "General Preferences",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(
                        icon = Icons.Default.Info,
                        title = "About WIBES Protect",
                        subtitle = "Version, architecture and cryptographic engine specs",
                        onClick = onOpenAboutApp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorderDark)

                    SettingRow(
                        icon = Icons.Default.Person,
                        title = "About Admin & Developer",
                        subtitle = "Developer contact, channel details and verification",
                        onClick = onOpenAboutAdmin
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorderDark)

                    SettingRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Protection History",
                        subtitle = "Purge cached records, generated APKs and logs",
                        onClick = { showClearConfirm = true }
                    )
                }
            }
        }

        item {
            CyberCard(glowColor = CyberCyan.copy(alpha = 0.15f)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Security Standards",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• APK Signature Scheme v2 & v3 supported\n• Advanced dex bytecode anti-tamper\n• String XOR decryption with random keys\n• Integrity checksum verification",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "Clear History?", color = Color.White) },
            text = { Text(text = "This will delete all saved scan results and protection history logs.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear All", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardDark
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
    }
}
