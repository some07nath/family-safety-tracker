package com.familysafety.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familysafety.tracker.data.repository.LocationRepository
import com.familysafety.tracker.ui.components.MemberStatusCard
import com.familysafety.tracker.ui.theme.AccentRed
import com.familysafety.tracker.ui.theme.PrimaryBlue

@Composable
fun FamilyListScreen(
    repository: LocationRepository,
    onSelectMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val familyMembers by repository.familyMembers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newRelationship by remember { mutableStateOf("") }
    var newLatitude by remember { mutableStateOf("22.5726") } // Default Kolkata
    var newLongitude by remember { mutableStateOf("88.3639") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Family Circle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${familyMembers.size} member(s) listed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Member")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions Row: Clear Demo Members
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = { repository.clearAllDemoMembers() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = AccentRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Demo Data", color = AccentRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Members List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(familyMembers, key = { it.id }) { member ->
                MemberStatusCard(
                    member = member,
                    onClick = { onSelectMember(member.id) },
                    onDelete = { repository.deleteFamilyMember(member.id) }
                )
            }
        }
    }

    // Dialog: Add Your Own Family Member
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Family Member",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add a family member to your circle to track them on your map.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name (e.g., Priya / Rahul)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newRelationship,
                        onValueChange = { newRelationship = it },
                        label = { Text("Relationship (e.g., Wife, Son, Brother)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newLatitude,
                            onValueChange = { newLatitude = it },
                            label = { Text("Latitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newLongitude,
                            onValueChange = { newLongitude = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "Defaults to Kolkata coordinates (22.5726, 88.3639)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = newLatitude.toDoubleOrNull() ?: 22.5726
                        val lng = newLongitude.toDoubleOrNull() ?: 88.3639
                        if (newName.isNotBlank()) {
                            repository.addFamilyMember(
                                name = newName.trim(),
                                relationship = newRelationship.trim().ifEmpty { "Family" },
                                latitude = lat,
                                longitude = lng
                            )
                            newName = ""
                            newRelationship = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
