package com.familysafety.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.familysafety.tracker.data.model.ActivityType
import com.familysafety.tracker.ui.theme.ChipStill
import com.familysafety.tracker.ui.theme.ChipVehicle
import com.familysafety.tracker.ui.theme.ChipWalking

@Composable
fun ActivityChip(
    activityType: ActivityType,
    speedKmh: Float = 0f,
    modifier: Modifier = Modifier
) {
    val (bgColor, icon) = when (activityType) {
        ActivityType.IN_VEHICLE -> Pair(ChipVehicle, Icons.Default.DirectionsCar)
        ActivityType.WALKING -> Pair(ChipWalking, Icons.Default.DirectionsWalk)
        ActivityType.RUNNING -> Pair(ChipWalking, Icons.Default.DirectionsRun)
        ActivityType.ON_BICYCLE -> Pair(ChipWalking, Icons.Default.DirectionsBike)
        else -> Pair(ChipStill, Icons.Default.Person)
    }

    Box(
        modifier = modifier
            .background(bgColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = activityType.displayName,
                tint = bgColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val text = if (speedKmh > 1f) {
                "${activityType.displayName} (${speedKmh.toInt()} km/h)"
            } else {
                activityType.displayName
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = bgColor
            )
        }
    }
}
