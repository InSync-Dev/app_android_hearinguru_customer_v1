package com.hearinguru.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private data class AppNotification(
    val icon: ImageVector,
    val iconBg: Int,
    val title: String,
    val body: String,
    val time: String,
    val isUnread: Boolean = false,
)


@Composable
fun NotificationsScreen() {
    val primaryContainer  = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val errorContainer    = MaterialTheme.colorScheme.errorContainer

    // Static demo notifications — in a real app these come from a ViewModel
    val notifications = listOf(
        AppNotification(
            icon        = Icons.Default.Hearing,
            iconBg      = 0,
            title       = "Take your weekly hearing check",
            body        = "It only takes 5 minutes. Track how your hearing changes over time.",
            time        = "Today",
            isUnread    = true,
        ),
        AppNotification(
            icon        = Icons.Default.VolumeMute,
            iconBg      = 1,
            title       = "Protect your ears today",
            body        = "Noise levels in your area are high. Consider using earplugs.",
            time        = "Yesterday",
            isUnread    = true,
        ),
        AppNotification(
            icon        = Icons.Default.SelfImprovement,
            iconBg      = 2,
            title       = "Tinnitus tip",
            body        = "Try 20 minutes of pink noise before bed. It helps many people fall asleep faster.",
            time        = "2 days ago",
        ),
        AppNotification(
            icon        = Icons.Default.MenuBook,
            iconBg      = 0,
            title       = "New article: Tinnitus causes & relief",
            body        = "Our audiologists published a comprehensive guide on managing tinnitus.",
            time        = "3 days ago",
        ),
        AppNotification(
            icon        = Icons.Default.MedicalServices,
            iconBg      = 3,
            title       = "Your free consultation is available",
            body        = "A HearinGuru specialist is ready to speak with you — no appointment needed.",
            time        = "1 week ago",
        ),
        AppNotification(
            icon        = Icons.Default.CheckCircle,
            iconBg      = 2,
            title       = "Hearing test complete",
            body        = "You took a hearing test. View your results in the Tools tab.",
            time        = "1 week ago",
        ),
    )

    val iconBgColors = listOf(
        primaryContainer,
        secondaryContainer,
        tertiaryContainer,
        errorContainer,
    )

    val unreadCount = notifications.count { it.isUnread }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // Header strip
        Surface(
            color    = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notifications",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (unreadCount > 0) {
                        Text(
                            "$unreadCount unread",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (unreadCount > 0) {
                    TextButton(onClick = {}) {
                        Text("Mark all read", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        notifications.forEachIndexed { index, notification ->
            val bgColor = if (notification.isUnread)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface

            Surface(
                color    = bgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Icon badge
                    Surface(
                        shape    = RoundedCornerShape(14.dp),
                        color    = iconBgColors[notification.iconBg],
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                notification.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint     = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Text content
                    Column(Modifier.weight(1f)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                notification.title,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                modifier   = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (notification.isUnread) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            notification.body,
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            notification.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (index < notifications.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
