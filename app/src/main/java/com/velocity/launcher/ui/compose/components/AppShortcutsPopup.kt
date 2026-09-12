package com.velocity.launcher.ui.compose.components

import android.content.pm.ShortcutInfo
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppNotificationModel

private val BOTTOM_SHEET_CORNER_RADIUS = 28.dp
private const val BOTTOM_SHEET_CONTAINER_ALPHA = 0.94f
private const val BOTTOM_SHEET_SCRIM_ALPHA = 0.40f

private val APP_ICON_SIZE = 44.dp
private val APP_ICON_CORNER_RADIUS = 12.dp

private val NOTIFICATION_CARD_CORNER_RADIUS = 16.dp
private const val NOTIFICATION_CARD_ALPHA = 0.60f
private val NOTIFICATION_DISMISS_BUTTON_SIZE = 24.dp
private val NOTIFICATION_DISMISS_ICON_SIZE = 16.dp

private val SHORTCUT_ICON_CONTAINER_SIZE = 32.dp
private val SHORTCUT_ICON_SIZE = 24.dp
private val SHORTCUT_CONTAINER_CORNER_RADIUS = 8.dp

private const val TITLE_SHORTCUTS = "Shortcuts"
private const val TITLE_NOTIFICATIONS = "Notifications"
private const val TEXT_ENABLE_NOTIFICATION_PREVIEWS = "Enable notification previews"
private const val TEXT_ENABLE_NOTIFICATION_PREVIEWS_DESC = "Allow notification access to see incoming alerts here"
private const val TEXT_DEFAULT_LAUNCHER_REQUIRED = "Default Launcher Required"
private const val TEXT_DEFAULT_LAUNCHER_REQUIRED_DESC = "Android requires Velocity to be set as your default launcher to show app quick actions."
private const val TEXT_EMPTY_SHORTCUTS_NOTIFICATIONS = "No notifications or shortcuts available"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShortcutsPopup(
    app: AppModel,
    shortcuts: List<ShortcutInfo>,
    notifications: List<AppNotificationModel>,
    hasShortcutHostPermission: Boolean = true,
    isNotificationAccessGranted: Boolean,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    getShortcutIconBitmap: (ShortcutInfo) -> ImageBitmap?,
    onShortcutClick: (ShortcutInfo) -> Unit,
    onNotificationClick: (AppNotificationModel) -> Unit,
    onDismissNotification: (AppNotificationModel) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iconBitmap = remember(app.id) { getIconBitmap(app) }
    var isNotificationsExpanded by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = BOTTOM_SHEET_CORNER_RADIUS, topEnd = BOTTOM_SHEET_CORNER_RADIUS),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = BOTTOM_SHEET_CONTAINER_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = BOTTOM_SHEET_SCRIM_ALPHA),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // App Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(APP_ICON_SIZE)
                            .clip(RoundedCornerShape(APP_ICON_CORNER_RADIUS))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(APP_ICON_SIZE)
                            .clip(RoundedCornerShape(APP_ICON_CORNER_RADIUS))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.label.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (notifications.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${notifications.size} New",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Notification Stream
            if (notifications.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isNotificationsExpanded = !isNotificationsExpanded }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TITLE_NOTIFICATIONS,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isNotificationsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isNotificationsExpanded) "Collapse Notifications" else "Expand Notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                AnimatedVisibility(visible = isNotificationsExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        notifications.forEach { notif ->
                            val timeSpan = remember(notif.postTime) {
                                DateUtils.getRelativeTimeSpanString(
                                    notif.postTime,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS
                                ).toString()
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS)
                                    )
                                    .clickable { onNotificationClick(notif) },
                                shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = NOTIFICATION_CARD_ALPHA)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (notif.title.isNotBlank()) {
                                            Text(
                                                text = notif.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }

                                        if (timeSpan.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = timeSpan,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (notif.isClearable) {
                                            IconButton(
                                                onClick = { onDismissNotification(notif) },
                                                modifier = Modifier.size(NOTIFICATION_DISMISS_BUTTON_SIZE)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(NOTIFICATION_DISMISS_ICON_SIZE)
                                                )
                                            }
                                        }
                                    }

                                    if (notif.text.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notif.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Permission Banner (if !isNotificationAccessGranted)
            if (!isNotificationAccessGranted) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS)
                        )
                        .clickable { onRequestNotificationAccess() },
                    shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = TEXT_ENABLE_NOTIFICATION_PREVIEWS,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TEXT_ENABLE_NOTIFICATION_PREVIEWS,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TEXT_ENABLE_NOTIFICATION_PREVIEWS_DESC,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Grant access",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Default Launcher Banner (if !hasShortcutHostPermission)
            if (!hasShortcutHostPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS)
                        )
                        .clickable { onOpenDefaultLauncherSettings() },
                    shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = TEXT_DEFAULT_LAUNCHER_REQUIRED,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TEXT_DEFAULT_LAUNCHER_REQUIRED,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TEXT_DEFAULT_LAUNCHER_REQUIRED_DESC,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Action Shortcut Tiles
            if (shortcuts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = TITLE_SHORTCUTS,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        shortcuts.forEach { shortcut ->
                            val sIconBitmap = remember(shortcut.id) { getShortcutIconBitmap(shortcut) }
                            val title = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onShortcutClick(shortcut) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(SHORTCUT_ICON_CONTAINER_SIZE)
                                        .clip(RoundedCornerShape(SHORTCUT_CONTAINER_CORNER_RADIUS))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sIconBitmap != null) {
                                        Image(
                                            bitmap = sIconBitmap,
                                            contentDescription = title,
                                            modifier = Modifier
                                                .size(SHORTCUT_ICON_SIZE)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Empty State Fallback
            val isEmpty = notifications.isEmpty() && shortcuts.isEmpty()
            if (isEmpty && hasShortcutHostPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS)
                        ),
                    shape = RoundedCornerShape(NOTIFICATION_CARD_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = TEXT_EMPTY_SHORTCUTS_NOTIFICATIONS,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
