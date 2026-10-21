package com.hearinguru.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hearinguru.app.data.UserProfileStore


private val AGE_GROUPS = listOf("Under 18", "18–40", "41–60", "61–80", "Over 80")

@Composable
fun ProfileScreen() {
    val context = LocalContext.current

    var notificationsEnabled  by remember { mutableStateOf(true) }
    var weeklyReminderEnabled by remember { mutableStateOf(true) }

    // Loaded from SharedPreferences; refreshes when dialog saves
    var savedAgeGroup   by remember { mutableStateOf(UserProfileStore.getAgeGroup(context)) }
    var isConsented     by remember { mutableStateOf(UserProfileStore.isConsented(context)) }
    var consentDate     by remember { mutableStateOf(UserProfileStore.getConsentDate(context)) }

    // Edit details dialog state
    var showEditDialog       by remember { mutableStateOf(false) }
    var editAgeGroupDraft    by remember { mutableStateOf(savedAgeGroup) }
    var showResetConfirm     by remember { mutableStateOf(false) }

    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary


    // Edit details dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Personal details", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Age group", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    AGE_GROUPS.forEach { group ->
                        val isSelected = editAgeGroupDraft == group
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .selectable(selected = isSelected, onClick = { editAgeGroupDraft = group })
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group, style = MaterialTheme.typography.bodyMedium)
                            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showResetConfirm = true },
                        colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset consent & start fresh", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    UserProfileStore.updateAgeGroup(context, editAgeGroupDraft)
                    savedAgeGroup = editAgeGroupDraft
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset consent confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon  = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset consent?", fontWeight = FontWeight.Bold) },
            text  = { Text("This clears your saved age group and consent. The next hearing test will ask for these details again.", style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        UserProfileStore.clearConsent(context)
                        savedAgeGroup  = ""
                        isConsented    = false
                        consentDate    = ""
                        editAgeGroupDraft = ""
                        showResetConfirm  = false
                        showEditDialog    = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {

        // Avatar + name hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.linearGradient(listOf(primaryColor, secondaryColor))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Initials avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "YO",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your Profile",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Manage your hearing health journey",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Personal details card — sourced from UserProfileStore
        ProfileSection(title = "Personal Details") {
            ProfileInfoTile(
                icon  = Icons.Default.Person,
                label = "Age group",
                value = savedAgeGroup.ifBlank { "Not set" }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ProfileInfoTile(
                icon       = Icons.Default.VerifiedUser,
                label      = "Consent given",
                value      = if (isConsented) "Yes" else "Not yet",
                valueColor = if (isConsented) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isConsented && consentDate.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoTile(
                    icon  = Icons.Default.CalendarToday,
                    label = "Consent date",
                    value = consentDate
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            // Edit button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    editAgeGroupDraft = savedAgeGroup
                    showEditDialog    = true
                }) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit details", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Hearing health card
        ProfileSection(title = "Hearing Health") {
            ProfileInfoTile(
                icon    = Icons.Default.Hearing,
                label   = "Last test result",
                value   = "Not yet taken"
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileInfoTile(
                icon  = Icons.Default.TrendingUp,
                label = "Tests completed",
                value = "0"
            )
        }

        Spacer(Modifier.height(12.dp))

        // Notifications
        ProfileSection(title = "Notifications") {
            ProfileToggleTile(
                icon    = Icons.Default.Notifications,
                label   = "Push notifications",
                subtext = "Reminders and health tips",
                isEnabled  = notificationsEnabled,
                onToggle   = { notificationsEnabled = it }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileToggleTile(
                icon    = Icons.Default.Schedule,
                label   = "Weekly hearing reminder",
                subtext = "Every Monday at 9 AM",
                isEnabled  = weeklyReminderEnabled,
                onToggle   = { weeklyReminderEnabled = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        // About & support
        ProfileSection(title = "About & Support") {
            ProfileNavTile(icon = Icons.Default.Info,         label = "About HearinGuru")
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileNavTile(icon = Icons.Default.Lock,         label = "Privacy Policy")
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileNavTile(icon = Icons.Default.HelpOutline,  label = "Help & FAQ")
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileNavTile(icon = Icons.Default.Star,         label = "Rate this app")
        }

        Spacer(Modifier.height(12.dp))

        // App version
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Hearing,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "HearinGuru",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Version 1.0.0 · Hearing health for everyone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier      = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}


@Composable
private fun ProfileInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}


@Composable
private fun ProfileToggleTile(
    icon: ImageVector,
    label: String,
    subtext: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = isEnabled,
            onCheckedChange = onToggle,
        )
    }
}


@Composable
private fun ProfileNavTile(
    icon: ImageVector,
    label: String,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}
