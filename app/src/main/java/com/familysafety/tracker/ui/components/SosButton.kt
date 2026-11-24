package com.familysafety.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familysafety.tracker.sos.EmergencySosManager
import com.familysafety.tracker.ui.theme.AccentRed
import kotlinx.coroutines.delay

@Composable
fun SosButton(
    latitude: Double,
    longitude: Double,
    batteryPct: Int,
    emergencyContacts: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }

    FloatingActionButton(
        onClick = {
            countdown = 5
            showConfirmationDialog = true
        },
        containerColor = AccentRed,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier
            .size(68.dp)
            .shadow(8.dp, CircleShape)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency SOS",
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "SOS",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }

    if (showConfirmationDialog) {
        LaunchedEffect(showConfirmationDialog) {
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            // Trigger SOS when countdown finishes
            EmergencySosManager.triggerSos(
                context = context,
                latitude = latitude,
                longitude = longitude,
                batteryPct = batteryPct,
                emergencyNumbers = emergencyContacts
            )
            showConfirmationDialog = false
        }

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = "🚨 SEND EMERGENCY SOS?",
                    fontWeight = FontWeight.Bold,
                    color = AccentRed
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Alerting family with your live location. If internet is down, an automatic SMS fallback will be sent.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AccentRed.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$countdown",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentRed
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sending in $countdown seconds...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        EmergencySosManager.triggerSos(
                            context = context,
                            latitude = latitude,
                            longitude = longitude,
                            batteryPct = batteryPct,
                            emergencyNumbers = emergencyContacts
                        )
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("SEND NOW")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
