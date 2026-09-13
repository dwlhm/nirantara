package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.WidgetGridPlacement
import com.velocity.launcher.ui.theme.AppLabelFontSize
import com.velocity.launcher.ui.theme.SoftTextShadow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AppListItem(
    app: AppModel,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onClick: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeftOrLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    isFavoriteItem: Boolean = false,
    activeEditingContainerKey: String? = null,
    widgetsRevision: Long = 0L,
    onSetActiveEditingContainer: (String?) -> Unit = {},
    getWidgetGridPlacement: (Int) -> WidgetGridPlacement? = { null },
    onSaveWidgetGridPlacement: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onResetWidgetGridPlacement: (Int) -> Unit = {},
    getWidgetCustomHeight: (Int) -> Int? = { null },
    onSaveWidgetCustomHeight: (Int, Int?) -> Unit = { _, _ -> },
    onConfigureWidgetClick: (Int, AppWidgetProviderInfo) -> Unit = { _, _ -> },
    onRemoveWidgetClick: (String, Int) -> Unit = { _, _ -> },
    onAddWidgetClick: ((AppModel) -> Unit)? = null,
    appWidgetHost: AppWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }

    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnSwipeRight = rememberUpdatedState(onSwipeRight)
    val currentOnSwipeLeftOrLongPress = rememberUpdatedState(onSwipeLeftOrLongPress)

    val iconBitmap = remember(app.id) { getIconBitmap(app) }
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .pointerInput(app.id) {
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downId = down.id
                            var isDrag = false
                            var isCancelled = false
                            var isUp = false
                            var totalDeltaX = 0f
                            var totalDeltaY = 0f
                            var longPressFired = false

                            val longPressJob = launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                if (!isDrag && !isCancelled) {
                                    longPressFired = true
                                    currentOnSwipeLeftOrLongPress.value()
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val current: PointerInputChange? = event.changes.firstOrNull { it.id == downId }
                                    if (current == null) {
                                        isCancelled = true
                                        break
                                    }

                                    if (current.isConsumed) {
                                        isCancelled = true
                                        break
                                    }

                                    if (current.changedToUp()) {
                                        isUp = true
                                        current.consume()
                                        break
                                    }

                                    val dragAmountX = current.position.x - current.previousPosition.x
                                    val dragAmountY = current.position.y - current.previousPosition.y
                                    totalDeltaX += dragAmountX
                                    totalDeltaY += dragAmountY

                                    if (!isDrag) {
                                        val absX = abs(totalDeltaX)
                                        val absY = abs(totalDeltaY)
                                        val touchSlop = viewConfiguration.touchSlop
                                        val horizontalSlop = touchSlop * 1.75f

                                        if ((absY > touchSlop * 0.75f && absY > absX * 0.8f) || absY > touchSlop) {
                                            isCancelled = true
                                            longPressJob.cancel()
                                            break
                                        } else if (absX > horizontalSlop && absX > absY * 2.0f) {
                                            isDrag = true
                                            longPressJob.cancel()
                                        }
                                    }

                                    if (isDrag) {
                                        current.consume()
                                        val newOffset = (offsetX.value + dragAmountX).coerceIn(
                                            -swipeThresholdPx * 1.5f,
                                            swipeThresholdPx * 1.5f
                                        )
                                        coroutineScope.launch {
                                            offsetX.snapTo(newOffset)
                                        }
                                    }
                                }
                            } finally {
                                longPressJob.cancel()
                            }

                            val finalOffset = offsetX.value
                            if (isDrag) {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring())
                                }
                                if (finalOffset > swipeThresholdPx) {
                                    currentOnSwipeRight.value()
                                } else if (finalOffset < -swipeThresholdPx) {
                                    currentOnSwipeLeftOrLongPress.value()
                                }
                            } else if (isUp && !isCancelled && !longPressFired &&
                                abs(totalDeltaX) <= viewConfiguration.touchSlop &&
                                abs(totalDeltaY) <= viewConfiguration.touchSlop
                            ) {
                                if (activeEditingContainerKey != null) {
                                    onSetActiveEditingContainer(null)
                                } else {
                                    currentOnClick.value()
                                }
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX.value
                    }
                    .fillMaxWidth()
                    .drawBehind {
                        val currentOffset = offsetX.value
                        val color = when {
                            currentOffset > 10f -> primaryContainerColor
                            currentOffset < -10f -> errorContainerColor
                            else -> Color.Transparent
                        }
                        if (color != Color.Transparent) {
                            drawRoundRect(
                                color = color,
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon (38dp)
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                        )
                    }

                    // Work profile badge indicator
                    if (app.isWorkProfile) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // App Label
                Text(
                    text = app.label,
                    style = LocalTextStyle.current.copy(shadow = SoftTextShadow),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = AppLabelFontSize,
                    fontWeight = if (isFavoriteItem) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Attached Pop-up Widget Indicator
                if (app.popupWidgetId != null && app.popupWidgetId != -1) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Widget attached",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Favorite Indicator Star
                if (app.isFavorite && !isFavoriteItem) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFD700).copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

