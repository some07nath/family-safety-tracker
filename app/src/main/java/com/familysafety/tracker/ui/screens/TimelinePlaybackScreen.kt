package com.familysafety.tracker.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.familysafety.tracker.data.model.ActivityType
import com.familysafety.tracker.data.model.LocationPoint
import com.familysafety.tracker.data.repository.LocationRepository
import com.familysafety.tracker.ui.theme.PrimaryBlue
import com.familysafety.tracker.ui.theme.SecondaryTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelinePlaybackScreen(
    repository: LocationRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Sample historic route points for demonstration (e.g., commute from Home to Office)
    val demoRoute = remember {
        listOf(
            LocationPoint(37.7749, -122.4194, speed = 0f, timestamp = 1700000000000L, activityType = ActivityType.STILL),
            LocationPoint(37.7758, -122.4180, speed = 15f, timestamp = 1700000300000L, activityType = ActivityType.IN_VEHICLE),
            LocationPoint(37.7780, -122.4140, speed = 35f, timestamp = 1700000600000L, activityType = ActivityType.IN_VEHICLE),
            LocationPoint(37.7820, -122.4100, speed = 48f, timestamp = 1700000900000L, activityType = ActivityType.IN_VEHICLE),
            LocationPoint(37.7850, -122.4060, speed = 20f, timestamp = 1700001200000L, activityType = ActivityType.IN_VEHICLE),
            LocationPoint(37.7885, -122.4010, speed = 4f, timestamp = 1700001500000L, activityType = ActivityType.WALKING),
            LocationPoint(37.7890, -122.4000, speed = 0f, timestamp = 1700001800000L, activityType = ActivityType.STILL)
        )
    }

    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }

    val currentIndex = (sliderProgress * (demoRoute.size - 1)).toInt().coerceIn(0, demoRoute.size - 1)
    val activePoint = demoRoute[currentIndex]
    val activeLatLng = LatLng(activePoint.latitude, activePoint.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(activeLatLng, 14.5f)
    }

    // Playback loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1200L)
            if (sliderProgress < 1f) {
                sliderProgress = (sliderProgress + (1f / (demoRoute.size - 1))).coerceAtMost(1f)
            } else {
                isPlaying = false
            }
        }
    }

    // Camera follow active point
    LaunchedEffect(currentIndex) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(activeLatLng))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Map Container
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // Route Polyline
                Polyline(
                    points = demoRoute.map { LatLng(it.latitude, it.longitude) },
                    color = PrimaryBlue,
                    width = 12f
                )

                // Start Marker
                Marker(
                    state = MarkerState(LatLng(demoRoute.first().latitude, demoRoute.first().longitude)),
                    title = "Start (Home)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )

                // End Marker
                Marker(
                    state = MarkerState(LatLng(demoRoute.last().latitude, demoRoute.last().longitude)),
                    title = "End (Office)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                // Active animated marker
                Marker(
                    state = MarkerState(activeLatLng),
                    title = "Current Playback Position",
                    snippet = "${activePoint.activityType.displayName} • ${activePoint.speed.toInt()} km/h",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        // Timeline Scrubber & Stats Controller
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Route Playback - Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activePoint.activityType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${activePoint.speed.toInt()} km/h", style = MaterialTheme.typography.bodyMedium)
                    }
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activePoint.timestamp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = timeStr, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider Scrubber
                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        sliderProgress = it
                        isPlaying = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue
                    )
                )

                // Play / Pause Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                }
            }
        }
    }
}
