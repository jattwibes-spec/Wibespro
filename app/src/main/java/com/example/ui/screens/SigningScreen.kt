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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeystoreConfig
import com.example.model.KeystoreMode
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberGradientButton
import com.example.ui.theme.*

@Composable
fun SigningScreen(
    config: KeystoreConfig,
    onUpdateConfig: (KeystoreConfig) -> Unit,
    onBrowseKeystoreFile: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "APK SIGNING CERTIFICATE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Configure cryptographic credentials for APK signature scheme v2/v3",
                fontSize = 12.sp,
                color = ElectricViolet
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            CyberCard(glowColor = PrimaryPurple.copy(alpha = 0.25f)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Signing Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onUpdateConfig(config.copy(mode = KeystoreMode.AUTO_GENERATE)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.mode == KeystoreMode.AUTO_GENERATE) PrimaryPurple else CardBorderDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Auto-Generate", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onUpdateConfig(config.copy(mode = KeystoreMode.IMPORT_EXISTING)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (config.mode == KeystoreMode.IMPORT_EXISTING) PrimaryPurple else CardBorderDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import JKS/BKS", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (config.mode == KeystoreMode.IMPORT_EXISTING) {
                        OutlinedTextField(
                            value = config.keystorePath,
                            onValueChange = { onUpdateConfig(config.copy(keystorePath = it)) },
                            label = { Text("Keystore File Path") },
                            trailingIcon = {
                                IconButton(onClick = onBrowseKeystoreFile) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Browse", tint = ElectricViolet)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("keystore_path_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = CardBorderDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = config.keyAlias,
                        onValueChange = { onUpdateConfig(config.copy(keyAlias = it)) },
                        label = { Text("Key Alias") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = config.keystorePassword,
                        onValueChange = { onUpdateConfig(config.copy(keystorePassword = it)) },
                        label = { Text("Store Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = config.keyPassword,
                        onValueChange = { onUpdateConfig(config.copy(keyPassword = it)) },
                        label = { Text("Key Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            CyberGradientButton(
                text = "Apply Certificate Settings",
                icon = Icons.Default.Check,
                onClick = { /* Settings Applied */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
