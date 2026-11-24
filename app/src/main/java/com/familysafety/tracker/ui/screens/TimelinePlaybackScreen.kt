package com.familysafety.tracker.ui.screens

import android.view.ViewGroup
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.familysafety.tracker.data.model.ActivityType
import com.familysafety.tracker.data.model.LocationPoint
import com.familysafety.tracker.data.repository.LocationRepository
import com.familysafety.tracker.ui.theme.PrimaryBlue
import com.familysafety.tracker.ui.theme.SecondaryTeal
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelinePlaybackScreen(
    repository: LocationRepository,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sample historic route points in Kolkata, India
    val demoRoute = remember {
        listOf(
            LocationPoint(22.5535, 88.3512, speed = 0f, timestamp = 1700000000000L, activityType = ActivityType.STILL), // Park Street
            LocationPoint(22.5600, 88.3520, speed = 18f, timestamp = 1700000300000L, activityType = ActivityType.IN_VEHICLE),
            LocationPoint(22.5670, 88.3510, speed = 32f, timestamp = 1700000600000L, activityType = ActivityType.IN_VEHICLE), // Esplanade
            LocationPoint(22.5750, 88.3480, speed = 25f, timestamp = 1700000900000L, activityType = ActivityType.IN_VEHICLE), // BBD Bagh
            LocationPoint(22.5850, 88.3426, speed = 15f, timestamp = 1700001200000L, activityType = ActivityType.IN_VEHICLE), // Howrah Bridge
            LocationPoint(22.5860, 88.3410, speed = 4f, timestamp = 1700001500000L, activityType = ActivityType.WALKING),
            LocationPoint(22.5870, 88.3400, speed = 0f, timestamp = 1700001800000L, activityType = ActivityType.STILL)  // Howrah Station
        )
    }

    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val currentIndex = (sliderProgress * (demoRoute.size - 1)).toInt().coerceIn(0, demoRoute.size - 1)
    val activePoint = demoRoute[currentIndex]
    val activeGeoPoint = GeoPoint(activePoint.latitude, activePoint.longitude)

    // Lifecycle observer for OSMDroid
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
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
        mapViewRef?.controller?.animateTo(activeGeoPoint)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // OpenStreetMap Canvas for Route Playback
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(demoRoute.first().latitude, demoRoute.first().longitude))
                        onResume()
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    mapViewRef = mapView
                    mapView.overlays.clear()

                    // Route Polyline
                    val polyline = Polyline(mapView).apply {
                        setPoints(demoRoute.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.parseColor("#1976D2")
                        outlinePaint.strokeWidth = 12f
                    }
                    mapView.overlays.add(polyline)

                    // Start Marker
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(demoRoute.first().latitude, demoRoute.first().longitude)
                        title = "Start (Park Street)"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(startMarker)

                    // End Marker
                    val endMarker = Marker(mapView).apply {
                        position = GeoPoint(demoRoute.last().latitude, demoRoute.last().longitude)
                        title = "End (Howrah)"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(endMarker)

                    // Active animated position marker
                    val activeMarker = Marker(mapView).apply {
                        position = activeGeoPoint
                        title = "Playback Position"
                        snippet = "${activePoint.activityType.displayName} • ${activePoint.speed.toInt()} km/h"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        showInfoWindow()
                    }
                    mapView.overlays.add(activeMarker)

                    mapView.invalidate()
                }
            )
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
                        text = "Route Playback - Kolkata",
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
