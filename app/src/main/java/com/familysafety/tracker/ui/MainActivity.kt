package com.familysafety.tracker.ui

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.familysafety.tracker.FamilySafetyApp
import com.familysafety.tracker.service.LocationTrackingService
import com.familysafety.tracker.ui.screens.FamilyListScreen
import com.familysafety.tracker.ui.screens.MapScreen
import com.familysafety.tracker.ui.screens.SettingsScreen
import com.familysafety.tracker.ui.screens.TimelinePlaybackScreen
import com.familysafety.tracker.ui.theme.FamilySafetyTheme
import com.familysafety.tracker.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private val emergencyContactsList = mutableStateListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            // Start background location service
            LocationTrackingService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("family_safety_prefs", Context.MODE_PRIVATE)
        loadEmergencyContacts()

        checkAndRequestPermissions()

        val repository = FamilySafetyApp.instance.repository

        setContent {
            FamilySafetyTheme {
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                                label = { Text("Map") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.People, contentDescription = "Family") },
                                label = { Text("Family") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.History, contentDescription = "Timeline") },
                                label = { Text("Timeline") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue)
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> MapScreen(
                            repository = repository,
                            emergencyContacts = emergencyContactsList,
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> FamilyListScreen(
                            repository = repository,
                            onSelectMember = { selectedTab = 0 },
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> TimelinePlaybackScreen(
                            repository = repository,
                            modifier = Modifier.padding(innerPadding)
                        )
                        3 -> SettingsScreen(
                            repository = repository,
                            emergencyContacts = emergencyContactsList,
                            onAddEmergencyContact = { addEmergencyContact(it) },
                            onRemoveEmergencyContact = { removeEmergencyContact(it) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            LocationTrackingService.startService(this)
        }
    }

    private fun loadEmergencyContacts() {
        val storedSet = prefs.getStringSet("emergency_contacts", setOf("+1234567890")) ?: emptySet()
        emergencyContactsList.clear()
        emergencyContactsList.addAll(storedSet)
    }

    private fun addEmergencyContact(contact: String) {
        if (!emergencyContactsList.contains(contact)) {
            emergencyContactsList.add(contact)
            saveEmergencyContacts()
        }
    }

    private fun removeEmergencyContact(contact: String) {
        emergencyContactsList.remove(contact)
        saveEmergencyContacts()
    }

    private fun saveEmergencyContacts() {
        prefs.edit().putStringSet("emergency_contacts", emergencyContactsList.toSet()).apply()
    }
}
