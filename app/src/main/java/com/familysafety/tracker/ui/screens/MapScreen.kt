package com.familysafety.tracker.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.familysafety.tracker.data.model.FamilyMember
import com.familysafety.tracker.data.repository.LocationRepository
import com.familysafety.tracker.ui.components.MemberStatusCard
import com.familysafety.tracker.ui.components.SosButton
import com.familysafety.tracker.ui.theme.AccentRed
import com.familysafety.tracker.ui.theme.PrimaryBlue
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    repository: LocationRepository,
    emergencyContacts: List<String>,
    modifier: Modifier = Modifier
) {
    val familyMembers by repository.familyMembers.collectAsState()
    val unsyncedCount by repository.unsyncedCountFlow.collectAsState(initial = 0)
    val isOnline = repository.isOnline()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }

    val initialCenter = remember(familyMembers) {
        val me = familyMembers.firstOrNull { it.isCurrentUser } ?: familyMembers.firstOrNull()
        if (me != null) GeoPoint(me.latitude, me.longitude) else GeoPoint(37.7749, -122.4194)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. OpenStreetMap (OSMDroid) Canvas - 100% Free, No API Key Required
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
                    controller.setCenter(initialCenter)
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                mapView.overlays.clear()

                familyMembers.forEach { member ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(member.latitude, member.longitude)
                        title = member.name
                        snippet = "${member.activityType.displayName} • Battery: ${member.batteryLevel}%"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            selectedMember = member
                            showInfoWindow()
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // 2. Offline / Sync Banner (Requirement #2)
        AnimatedVisibility(
            visible = !isOnline || unsyncedCount > 0,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (!isOnline) AccentRed.copy(alpha = 0.95f) else PrimaryBlue.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.Sync,
                        contentDescription = "Sync State",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!isOnline) {
                            "Offline: Storing data locally ($unsyncedCount queued)"
                        } else {
                            "Uploading $unsyncedCount stored location points..."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 3. Center Camera FAB
        FloatingActionButton(
            onClick = {
                val me = familyMembers.firstOrNull { it.isCurrentUser } ?: familyMembers.firstOrNull()
                if (me != null) {
                    mapViewRef?.controller?.animateTo(
                        GeoPoint(me.latitude, me.longitude),
                        16.0,
                        800L
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryBlue
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center on Me")
        }

        // 4. Emergency SOS Button (with SMS Fallback)
        val myInfo = familyMembers.firstOrNull { it.isCurrentUser }
        SosButton(
            latitude = myInfo?.latitude ?: 0.0,
            longitude = myInfo?.longitude ?: 0.0,
            batteryPct = myInfo?.batteryLevel ?: 100,
            emergencyContacts = emergencyContacts,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 180.dp, start = 16.dp)
        )

        // 5. Family Members Horizontal Carousel (Bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(bottom = 16.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                items(familyMembers) { member ->
                    Box(modifier = Modifier.width(280.dp).padding(horizontal = 6.dp)) {
                        MemberStatusCard(
                            member = member,
                            onClick = {
                                selectedMember = member
                                mapViewRef?.controller?.animateTo(
                                    GeoPoint(member.latitude, member.longitude),
                                    16.0,
                                    800L
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
