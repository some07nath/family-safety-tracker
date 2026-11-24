package com.familysafety.tracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familysafety.tracker.ui.theme.BatteryGreen
import com.familysafety.tracker.ui.theme.BatteryOrange
import com.familysafety.tracker.ui.theme.BatteryRed

@Composable
fun BatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        batteryLevel < 20 -> BatteryRed
        batteryLevel < 50 -> BatteryOrange
        else -> BatteryGreen
    }

    val icon = when {
        isCharging -> Icons.Default.BatteryChargingFull
        batteryLevel < 20 -> Icons.Default.BatteryAlert
        else -> Icons.Default.BatteryFull
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Battery Status",
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$batteryLevel%",
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
