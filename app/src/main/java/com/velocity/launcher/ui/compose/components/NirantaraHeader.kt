package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.BatteryState
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
    topWidgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onClockClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onRemoveTopWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
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

        // Optional Top Widget Slot
        if (topWidgetId != -1) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    .padding(8.dp)
            ) {
                WidgetHostContainer(
                    appWidgetId = topWidgetId,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager
                )

                // Quick remove top widget button
                IconButton(
                    onClick = onRemoveTopWidgetClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Widget",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
