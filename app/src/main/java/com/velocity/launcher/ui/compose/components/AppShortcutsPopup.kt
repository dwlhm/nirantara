package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ShortcutInfo
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppNotificationModel
import com.velocity.launcher.ui.theme.SoftTextShadow

@Composable
fun AppShortcutsPopup(
    app: AppModel,
    shortcuts: List<ShortcutInfo>,
    notifications: List<AppNotificationModel>,
    hasShortcutHostPermission: Boolean = true,
    isNotificationAccessGranted: Boolean,
    focalOffsetDp: Dp = 180.dp,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    getShortcutIconBitmap: (ShortcutInfo) -> ImageBitmap?,
    getWidgetCustomHeight: (widgetId: Int) -> Int? = { null },
    onSaveWidgetCustomHeight: (widgetId: Int, heightDp: Int?) -> Unit = { _, _ -> },
    onShortcutClick: (ShortcutInfo) -> Unit,
    onNotificationClick: (AppNotificationModel) -> Unit,
    onDismissNotification: (AppNotificationModel) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit = {},
    onAddWidgetClick: () -> Unit,
    onConfigureWidgetClick: (widgetId: Int, providerInfo: AppWidgetProviderInfo) -> Unit,
    onRemoveWidgetClick: (widgetId: Int) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onDismissRequest: () -> Unit
) {
    val iconBitmap = remember(app.id) { getIconBitmap(app) }
    var isEditMode by remember { mutableStateOf(false) }
    var isNotificationsExpanded by remember { mutableStateOf(true) }
    val contentHorizontalPadding = 40.dp

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 50.dp.toPx() }
    var accumulatedPullDownPx by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()

    val dismissNestedScrollConnection = remember(dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && scrollState.value == 0) {
                    accumulatedPullDownPx += available.y
                    if (accumulatedPullDownPx >= dismissThresholdPx) {
                        accumulatedPullDownPx = 0f
                        onDismissRequest()
                    }
                } else if (available.y < 0f) {
                    accumulatedPullDownPx = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    accumulatedPullDownPx += available.y
                    if (accumulatedPullDownPx >= dismissThresholdPx) {
                        accumulatedPullDownPx = 0f
                        onDismissRequest()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                accumulatedPullDownPx = 0f
                if (available.y > 600f && scrollState.value == 0) {
                    onDismissRequest()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    // Progressive render delay to ensure frame 0 instant transition
    var isProgressiveReady by remember(app.id) { mutableStateOf(false) }
    LaunchedEffect(app.id) {
        kotlinx.coroutines.delay(40L)
        isProgressiveReady = true
    }

    val hasAvailableWidgets = remember(app.packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appWidgetManager.getInstalledProvidersForPackage(app.packageName, app.userHandle).isNotEmpty()
            } else {
                appWidgetManager.installedProviders.any { it.provider.packageName == app.packageName }
            }
        } catch (e: Exception) {
            false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .nestedScroll(dismissNestedScrollConnection)
                .verticalScroll(scrollState)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(focalOffsetDp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { accumulatedPullDownPx = 0f },
                            onDragCancel = { accumulatedPullDownPx = 0f }
                        ) { change, dragAmount ->
                            if (dragAmount > 0f) {
                                accumulatedPullDownPx += dragAmount
                                if (accumulatedPullDownPx >= dismissThresholdPx) {
                                    accumulatedPullDownPx = 0f
                                    change.consume()
                                    onDismissRequest()
                                }
                            }
                        }
                    }
            )

            // 1. Top Bar / Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = app.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasAvailableWidgets) {
                    IconButton(
                        onClick = onAddWidgetClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Add Widget",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Popup Settings",
                        tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Notifications
            if (notifications.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isNotificationsExpanded = !isNotificationsExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications (${notifications.size})",
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
                    Spacer(modifier = Modifier.height(4.dp))

                    AnimatedVisibility(visible = isNotificationsExpanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNotificationClick(notif) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    tonalElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
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
                                                Text(
                                                    text = timeSpan,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }

                                            if (notif.isClearable) {
                                                IconButton(
                                                    onClick = { onDismissNotification(notif) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (notif.text.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = notif.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 2. Enable Notification Permission button (Visible ONLY when isEditMode && !isNotificationAccessGranted)
            if (isEditMode && !isNotificationAccessGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { onRequestNotificationAccess() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Enable notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enable notification previews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Default Launcher Permission Required Banner (if permission not granted)
            if (!hasShortcutHostPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenDefaultLauncherSettings() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Default Launcher Required",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Launcher Required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android requires Velocity to be set as your default launcher to show app quick actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. App Shortcuts Section (if present)
            if (shortcuts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding)
                ) {
                    Text(
                        text = "Shortcuts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    shortcuts.forEach { shortcut ->
                        val sIconBitmap = remember(shortcut.id) { getShortcutIconBitmap(shortcut) }
                        val title = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onShortcutClick(shortcut) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (sIconBitmap != null) {
                                Image(
                                    bitmap = sIconBitmap,
                                    contentDescription = title,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Attached Widgets Section (Full Width, 0 horizontal padding):
            if (app.popupWidgetIds.isNotEmpty()) {
                Text(
                    text = if (app.popupWidgetIds.size > 1) "Pop-up Widgets" else "Pop-up Widget",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = contentHorizontalPadding)
                )
                Spacer(modifier = Modifier.height(6.dp))

                app.popupWidgetIds.forEach { widgetId ->
                    if (!isProgressiveReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    } else {
                        val providerInfo = remember(widgetId) { appWidgetManager.getAppWidgetInfo(widgetId) }
                        var currentHeightDp by remember(widgetId) { mutableStateOf(getWidgetCustomHeight(widgetId)) }
                        var isDragging by remember { mutableStateOf(false) }
                        val density = LocalDensity.current.density

                        if (isEditMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Main widget frame with accent border
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        )
                                ) {
                                    WidgetHostContainer(
                                        appWidgetId = widgetId,
                                        appWidgetHost = appWidgetHost,
                                        appWidgetManager = appWidgetManager,
                                        customHeightDp = currentHeightDp
                                    )
                                }

                                // 4 Corner dots
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopStart)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.BottomStart)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )

                                // Side indicator dots / handles (Left and Right midpoint circles)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.CenterStart)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.CenterEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )

                                // Top Grab Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                onDragStart = { isDragging = true },
                                                onDragEnd = {
                                                    isDragging = false
                                                    onSaveWidgetCustomHeight(widgetId, currentHeightDp)
                                                },
                                                onDragCancel = { isDragging = false }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                val deltaDp = (dragAmount / density).toInt()
                                                val base = currentHeightDp ?: 160
                                                currentHeightDp = (base - deltaDp).coerceIn(60, 800)
                                            }
                                        }
                                        .size(width = 36.dp, height = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 2.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }

                                // Bottom Grab Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                onDragStart = { isDragging = true },
                                                onDragEnd = {
                                                    isDragging = false
                                                    onSaveWidgetCustomHeight(widgetId, currentHeightDp)
                                                },
                                                onDragCancel = { isDragging = false }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                val deltaDp = (dragAmount / density).toInt()
                                                val base = currentHeightDp ?: 160
                                                currentHeightDp = (base + deltaDp).coerceIn(60, 800)
                                            }
                                        }
                                        .size(width = 36.dp, height = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 2.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }

                                // Size Badge (While dragging or in edit mode when custom height is set)
                                if (isDragging || currentHeightDp != null) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 12.dp, top = 8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.75f)
                                    ) {
                                        Text(
                                            text = "${currentHeightDp ?: 160} dp",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Action Overlay (Reset Size, Configure, Remove)
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentHeightDp != null) {
                                        IconButton(
                                            onClick = {
                                                currentHeightDp = null
                                                onSaveWidgetCustomHeight(widgetId, null)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Reset Size",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (providerInfo?.configure != null) {
                                        IconButton(
                                            onClick = { onConfigureWidgetClick(widgetId, providerInfo) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Configure Widget",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onRemoveWidgetClick(widgetId) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Widget",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            WidgetHostContainer(
                                appWidgetId = widgetId,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                customHeightDp = currentHeightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 6. Add Widget Button in Edit Mode
            if (isEditMode && hasAvailableWidgets) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable { onAddWidgetClick() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Attach Pop-up Widget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "+ Attach Pop-up Widget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 7. Empty State Fallback (when empty and NOT in edit mode)
            val isEmpty = notifications.isEmpty() && shortcuts.isEmpty() && app.popupWidgetIds.isEmpty()
            if (isEmpty && !isEditMode && hasShortcutHostPermission) {
                Text(
                    text = "No notifications or widgets available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = contentHorizontalPadding, vertical = 8.dp)
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(focalOffsetDp + 60.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
        }
    }
}
