package com.velocity.launcher.ui.compose.components

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsBottomSheet(
    app: AppModel,
    shortcuts: List<ShortcutInfo> = emptyList(),
    hasShortcutHostPermission: Boolean = true,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    getShortcutIconBitmap: (ShortcutInfo) -> ImageBitmap? = { null },
    onShortcutClick: (ShortcutInfo) -> Unit = {},
    onOpenDefaultLauncherSettings: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onAttachOrChangeWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onHideAppClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val iconBitmap = remember(app.id) { getIconBitmap(app) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // App Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions / Shortcuts Section (if present)
            if (shortcuts.isNotEmpty()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                shortcuts.forEach { shortcut ->
                    val sIconBitmap = remember(shortcut.id) { getShortcutIconBitmap(shortcut) }
                    val title = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShortcutClick(shortcut) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sIconBitmap != null) {
                            Image(
                                bitmap = sIconBitmap,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Set as Default Launcher item (when permission not granted)
            if (!hasShortcutHostPermission) {
                OptionItem(
                    icon = Icons.Default.Tune,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Set as Default Launcher (Enables Quick Actions)",
                    onClick = onOpenDefaultLauncherSettings
                )
            }

            // 1. Pin / Unpin from Favorites
            OptionItem(
                icon = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                iconTint = if (app.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                title = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = onToggleFavorite
            )

            // 2. Attach / Add Widget
            OptionItem(
                icon = Icons.Default.Widgets,
                iconTint = MaterialTheme.colorScheme.primary,
                title = if (app.popupWidgetIds.isNotEmpty()) {
                    "Add Widget"
                } else {
                    "Attach Widget"
                },
                onClick = onAttachOrChangeWidget
            )

            // Remove widget option if configured
            if (app.popupWidgetIds.isNotEmpty()) {
                OptionItem(
                    icon = Icons.Default.DeleteOutline,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = if (app.popupWidgetIds.size > 1) "Remove All Widgets" else "Remove Widget",
                    onClick = onRemoveWidget
                )
            }

            // 3. App Info
            OptionItem(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "App Info",
                onClick = onAppInfoClick
            )

            // 4. Uninstall App
            OptionItem(
                icon = Icons.Default.Delete,
                iconTint = MaterialTheme.colorScheme.error,
                title = "Uninstall",
                onClick = onUninstallClick
            )

            // 5. Hide App
            OptionItem(
                icon = Icons.Default.VisibilityOff,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Hide App",
                onClick = onHideAppClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
