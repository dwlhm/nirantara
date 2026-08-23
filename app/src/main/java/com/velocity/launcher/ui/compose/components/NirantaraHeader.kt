package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.BatteryState
import com.velocity.launcher.data.WidgetGridPlacement
import com.velocity.launcher.ui.theme.ClockTextShadow
import com.velocity.launcher.ui.theme.SoftTextShadow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigitalClock(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val delayMs = (60_000L - (System.currentTimeMillis() % 60_000L)).coerceAtLeast(1000L)
            delay(delayMs)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(currentTime) { timeFormat.format(Date(currentTime)) }

    Text(
        text = formattedTime,
        style = LocalTextStyle.current.copy(shadow = ClockTextShadow),
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1).sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            )
    )
}

@Composable
fun DateBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val delayMs = (60_000L - (System.currentTimeMillis() % 60_000L)).coerceAtLeast(1000L)
            delay(delayMs)
            currentTime = System.currentTimeMillis()
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val formattedDate = remember(currentTime) { dateFormat.format(Date(currentTime)) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = formattedDate,
            style = LocalTextStyle.current.copy(shadow = SoftTextShadow),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BatteryBadge(
    batteryState: BatteryState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (batteryState.isCharging || batteryState.isPlugged) {
                Icons.Default.BatteryChargingFull
            } else {
                Icons.Default.BatteryStd
            },
            contentDescription = null,
            tint = if (batteryState.isCharging) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${batteryState.level}%",
            style = LocalTextStyle.current.copy(shadow = SoftTextShadow),
            color = if (batteryState.isCharging) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NirantaraHeader(
    batteryState: BatteryState,
    topWidgetIds: List<Int> = emptyList(),
    topWidgetId: Int = -1,
    activeEditingContainerKey: String? = null,
    widgetsRevision: Long = 0L,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onClockClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAddTopWidgetClick: () -> Unit = {},
    onRemoveTopWidgetClick: (widgetId: Int) -> Unit = {},
    onSetActiveEditingContainer: (String?) -> Unit = {},
    onConfigureWidgetClick: (widgetId: Int, providerInfo: AppWidgetProviderInfo) -> Unit = { _, _ -> },
    getWidgetGridPlacement: (widgetId: Int) -> WidgetGridPlacement? = { null },
    onSaveWidgetGridPlacement: (widgetId: Int, row: Int, startCol: Int, span: Int) -> Unit = { _, _, _, _ -> },
    onResetWidgetGridPlacement: (widgetId: Int) -> Unit = {},
    getWidgetCustomHeight: (widgetId: Int) -> Int? = { null },
    onSaveWidgetCustomHeight: (widgetId: Int, heightDp: Int?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val resolvedWidgetIds = remember(topWidgetIds, topWidgetId) {
        if (topWidgetIds.isNotEmpty()) {
            topWidgetIds
        } else if (topWidgetId != -1) {
            listOf(topWidgetId)
        } else {
            emptyList()
        }
    }

    val isContainerEditing = activeEditingContainerKey == "header"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(activeEditingContainerKey) {
                detectTapGestures(
                    onLongPress = {
                        onSetActiveEditingContainer("header")
                    }
                )
            }
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Digital Clock (isolated sub-composable)
            DigitalClock(onClick = onClockClick)

            Spacer(modifier = Modifier.height(4.dp))

            // Date and Battery Row (isolated sub-composables)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                DateBadge(onClick = onCalendarClick)

                Spacer(modifier = Modifier.width(16.dp))

                BatteryBadge(batteryState = batteryState)
            }
        }

        // Top Header Multi-Widget Flow Grid
        if (resolvedWidgetIds.isNotEmpty() || isContainerEditing) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                WidgetFlowGrid(
                    widgetIds = resolvedWidgetIds,
                    isContainerEditing = isContainerEditing,
                    widgetsRevision = widgetsRevision,
                    onTriggerContainerEdit = { onSetActiveEditingContainer("header") },
                    getWidgetGridPlacement = getWidgetGridPlacement,
                    onSaveWidgetGridPlacement = onSaveWidgetGridPlacement,
                    onResetWidgetGridPlacement = onResetWidgetGridPlacement,
                    getWidgetCustomHeight = getWidgetCustomHeight,
                    onSaveWidgetCustomHeight = onSaveWidgetCustomHeight,
                    onConfigureWidgetClick = onConfigureWidgetClick,
                    onRemoveWidgetClick = onRemoveTopWidgetClick,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    showAddWidgetButton = isContainerEditing,
                    onAddWidgetClick = onAddTopWidgetClick,
                    onFinishEditing = { onSetActiveEditingContainer(null) }
                )
            }
        }
    }
}
