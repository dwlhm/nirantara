package com.velocity.launcher.ui.compose.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.WidgetGridPlacement

data class GridWidgetItem(
    val widgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val row: Int,
    val startCol: Int, // 0..7
    val span: Int, // 1..8 (startCol + span <= 8)
    val customHeightDp: Int? = null
)

object WidgetGridLayoutEngine {
    fun isOverlapping(start1: Int, span1: Int, start2: Int, span2: Int): Boolean {
        val end1 = start1 + span1 - 1
        val end2 = start2 + span2 - 1
        return maxOf(start1, start2) <= minOf(end1, end2)
    }

    fun findFirstAvailableSlot(
        span: Int,
        existingItems: List<GridWidgetItem>,
        preferredStartCol: Int = 0
    ): Int? {
        val clampedSpan = span.coerceIn(1, 8)
        val maxStart = 8 - clampedSpan
        if (maxStart < 0) return null

        val validPreferred = preferredStartCol.coerceIn(0, maxStart)
        val collidesPreferred = existingItems.any { other ->
            isOverlapping(validPreferred, clampedSpan, other.startCol, other.span)
        }
        if (!collidesPreferred) return validPreferred

        return (0..maxStart).firstOrNull { col ->
            existingItems.none { other ->
                isOverlapping(col, clampedSpan, other.startCol, other.span)
            }
        }
    }

    private tailrec fun packRowRecursive(
        rowIndex: Int,
        maxRow: Int,
        rowGroups: Map<Int, List<GridWidgetItem>>,
        pendingOverflow: List<GridWidgetItem>,
        accumulatedRows: List<List<GridWidgetItem>>
    ): List<List<GridWidgetItem>> {
        if (rowIndex > maxRow && pendingOverflow.isEmpty()) {
            return accumulatedRows
        }

        val candidates = pendingOverflow + (rowGroups[rowIndex] ?: emptyList())

        data class RowPlacement(
            val placed: List<GridWidgetItem> = emptyList(),
            val nextOverflow: List<GridWidgetItem> = emptyList()
        )

        val result = candidates.fold(RowPlacement()) { acc, cand ->
            val validSpan = cand.span.coerceIn(1, 8)
            val preferredStart = cand.startCol.coerceIn(0, 8 - validSpan)
            val targetStart = findFirstAvailableSlot(
                span = validSpan,
                existingItems = acc.placed,
                preferredStartCol = preferredStart
            )

            if (targetStart != null) {
                acc.copy(
                    placed = acc.placed + cand.copy(
                        row = accumulatedRows.size,
                        startCol = targetStart,
                        span = validSpan
                    )
                )
            } else {
                acc.copy(
                    nextOverflow = acc.nextOverflow + cand.copy(row = rowIndex + 1)
                )
            }
        }

        val nextAccumulated = if (result.placed.isNotEmpty()) {
            accumulatedRows + listOf(result.placed.sortedBy { it.startCol })
        } else {
            accumulatedRows
        }

        return packRowRecursive(
            rowIndex = rowIndex + 1,
            maxRow = maxRow,
            rowGroups = rowGroups,
            pendingOverflow = result.nextOverflow,
            accumulatedRows = nextAccumulated
        )
    }

    fun packWidgetsIntoRows(
        items: List<GridWidgetItem>
    ): List<List<GridWidgetItem>> {
        if (items.isEmpty()) return emptyList()
        val rowGroups = items.groupBy { it.row.coerceAtLeast(0) }
        val maxRow = rowGroups.keys.maxOrNull() ?: 0
        return packRowRecursive(
            rowIndex = 0,
            maxRow = maxRow,
            rowGroups = rowGroups,
            pendingOverflow = emptyList(),
            accumulatedRows = emptyList()
        )
    }
}

@Composable
fun WidgetFlowGrid(
    widgetIds: List<Int>,
    isContainerEditing: Boolean,
    onTriggerContainerEdit: () -> Unit,
    getWidgetGridPlacement: (Int) -> WidgetGridPlacement?,
    onSaveWidgetGridPlacement: (widgetId: Int, row: Int, startCol: Int, span: Int) -> Unit,
    onResetWidgetGridPlacement: (Int) -> Unit,
    getWidgetCustomHeight: (Int) -> Int?,
    onSaveWidgetCustomHeight: (Int, Int?) -> Unit,
    onConfigureWidgetClick: (Int, AppWidgetProviderInfo) -> Unit,
    onRemoveWidgetClick: (Int) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    widgetsRevision: Long = 0L,
    modifier: Modifier = Modifier,
    showAddWidgetButton: Boolean = false,
    onAddWidgetClick: (() -> Unit)? = null,
    onFinishEditing: (() -> Unit)? = null
) {
    if (widgetIds.isEmpty() && !((showAddWidgetButton || isContainerEditing) && (onAddWidgetClick != null || onFinishEditing != null))) return

    val validItems = remember(widgetIds, widgetsRevision) {
        widgetIds.mapNotNull { id ->
            val info = appWidgetManager.getAppWidgetInfo(id) ?: return@mapNotNull null
            val gridPlacement = getWidgetGridPlacement(id)
            val customHeight = getWidgetCustomHeight(id)
            val (row, startCol, span) = if (gridPlacement != null) {
                val validRow = gridPlacement.row.coerceAtLeast(0)
                val validStart = gridPlacement.startCol.coerceIn(0, 7)
                val validSpan = gridPlacement.span.coerceIn(1, 8 - validStart)
                Triple(validRow, validStart, validSpan)
            } else {
                val defaultSpan = if (info.minWidth <= 180) 4 else 8
                Triple(0, 0, defaultSpan.coerceIn(1, 8))
            }
            GridWidgetItem(
                widgetId = id,
                providerInfo = info,
                row = row,
                startCol = startCol,
                span = span,
                customHeightDp = customHeight
            )
        }
    }

    // 2D Row Grouping & Layout
    val rows = remember(validItems) {
        WidgetGridLayoutEngine.packWidgetsIntoRows(validItems)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var currentColumnIndex = 0

                for (item in row) {
                    key(item.widgetId) {
                        val widgetId = item.widgetId
                        val providerInfo = item.providerInfo
                        val itemRow = item.row
                        val itemStartCol = item.startCol
                        val itemSpan = item.span
                        val isEditing = isContainerEditing

                        val currentItem by rememberUpdatedState(item)
                        val currentRow by rememberUpdatedState(row)
                        val currentRows by rememberUpdatedState(rows)
                        val currentOnSavePlacement by rememberUpdatedState(onSaveWidgetGridPlacement)
                        val currentOnSaveHeight by rememberUpdatedState(onSaveWidgetCustomHeight)
                        val currentOnResetPlacement by rememberUpdatedState(onResetWidgetGridPlacement)
                        val currentOnConfigureClick by rememberUpdatedState(onConfigureWidgetClick)
                        val currentOnRemoveClick by rememberUpdatedState(onRemoveWidgetClick)

                        var currentHeightDp by remember(widgetId, item.customHeightDp) {
                            mutableStateOf(item.customHeightDp)
                        }
                        var isDraggingHeight by remember { mutableStateOf(false) }
                        var accumulatedHeightDp by remember(widgetId) { mutableFloatStateOf(0f) }
                        var moveDragOffsetX by remember(widgetId) { mutableFloatStateOf(0f) }
                        var targetStartCol by remember(widgetId, itemStartCol) { mutableStateOf(itemStartCol) }
                        var leftDragAccumulator by remember(widgetId) { mutableFloatStateOf(0f) }
                        var rightDragAccumulator by remember(widgetId) { mutableFloatStateOf(0f) }
                        var itemWidthPx by remember(widgetId) { mutableFloatStateOf(0f) }
                        val density = LocalDensity.current.density

                        LaunchedEffect(item.customHeightDp) {
                            if (!isDraggingHeight) {
                                currentHeightDp = item.customHeightDp
                            }
                        }

                        // If item.startCol > currentColumnIndex, insert leading Spacer
                        if (itemStartCol > currentColumnIndex) {
                            val leadingSpacerWeight = itemStartCol - currentColumnIndex
                            Spacer(modifier = Modifier.weight(leadingSpacerWeight.toFloat()))
                            currentColumnIndex = itemStartCol
                        }

                        val colWidthPx = if (itemSpan > 0 && itemWidthPx > 0f) {
                            itemWidthPx / itemSpan.toFloat()
                        } else {
                            density * 44f
                        }

                        Box(
                            modifier = Modifier
                                .weight(itemSpan.toFloat())
                                .wrapContentHeight()
                                .then(
                                    if (isEditing) {
                                        Modifier
                                            .graphicsLayer {
                                                val maxLeftPx = -itemStartCol * colWidthPx
                                                val maxRightPx = (8 - (itemStartCol + itemSpan)) * colWidthPx
                                                translationX = moveDragOffsetX.coerceIn(maxLeftPx, maxRightPx)
                                            }
                                            .border(
                                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .onGloballyPositioned { coordinates ->
                                                itemWidthPx = coordinates.size.width.toFloat()
                                            }
                                    } else {
                                        Modifier
                                            .pointerInput(widgetId) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        onTriggerContainerEdit()
                                                    }
                                                )
                                            }
                                    }
                                )
                        ) {
                            // Widget Content Container
                            WidgetHostContainer(
                                appWidgetId = widgetId,
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager,
                                span = itemSpan,
                                customHeightDp = currentHeightDp,
                                isEditing = isEditing,
                                onLongPress = onTriggerContainerEdit,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // In-Situ Direct-Gesture Editing Controls Overlay
                            if (isEditing) {
                                // 1. Drag-to-Move Gesture Detector on Widget Body
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(start = 28.dp, end = 28.dp, bottom = 36.dp)
                                        .pointerInput(widgetId) {
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    moveDragOffsetX = 0f
                                                    targetStartCol = currentItem.startCol
                                                },
                                                onDragEnd = {
                                                    val finalStart = targetStartCol
                                                    moveDragOffsetX = 0f
                                                    if (finalStart != currentItem.startCol) {
                                                        currentOnSavePlacement(widgetId, currentItem.row, finalStart, currentItem.span)
                                                    }
                                                },
                                                onDragCancel = {
                                                    moveDragOffsetX = 0f
                                                    targetStartCol = currentItem.startCol
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                moveDragOffsetX += dragAmount
                                                val step = (colWidthPx).coerceAtLeast(20f)
                                                val colDelta = (moveDragOffsetX / step).toInt()
                                                val proposedStart = (currentItem.startCol + colDelta).coerceIn(0, 8 - currentItem.span)
                                                val isColliding = currentRow.any { other ->
                                                    other.widgetId != widgetId &&
                                                    WidgetGridLayoutEngine.isOverlapping(proposedStart, currentItem.span, other.startCol, other.span)
                                                }
                                                if (!isColliding) {
                                                    targetStartCol = proposedStart
                                                }
                                            }
                                        }
                                )

                                // 4 Corner Accent Dots
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

                                // 2. Left Edge Resize Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(width = 28.dp, height = 48.dp)
                                        .pointerInput(widgetId) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { leftDragAccumulator = 0f },
                                                onDragEnd = { leftDragAccumulator = 0f },
                                                onDragCancel = { leftDragAccumulator = 0f }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                leftDragAccumulator += dragAmount
                                                val stepPx = (colWidthPx * 0.5f).coerceAtLeast(20.dp.toPx())
                                                val curItem = currentItem
                                                val currentEnd = curItem.startCol + curItem.span - 1

                                                if (leftDragAccumulator <= -stepPx) {
                                                    // Expand to the left
                                                    if (curItem.startCol > 0) {
                                                        val proposedStart = curItem.startCol - 1
                                                        val proposedSpan = currentEnd - proposedStart + 1
                                                        val hasCollision = currentRow.any { other ->
                                                            other.widgetId != widgetId &&
                                                            WidgetGridLayoutEngine.isOverlapping(proposedStart, proposedSpan, other.startCol, other.span)
                                                        }
                                                        if (!hasCollision) {
                                                            leftDragAccumulator += stepPx
                                                            currentOnSavePlacement(widgetId, curItem.row, proposedStart, proposedSpan)
                                                        }
                                                    }
                                                } else if (leftDragAccumulator >= stepPx) {
                                                    // Contract from the left
                                                    if (curItem.span > 1 && curItem.startCol < currentEnd) {
                                                        val proposedStart = curItem.startCol + 1
                                                        val proposedSpan = currentEnd - proposedStart + 1
                                                        val hasCollision = currentRow.any { other ->
                                                            other.widgetId != widgetId &&
                                                            WidgetGridLayoutEngine.isOverlapping(proposedStart, proposedSpan, other.startCol, other.span)
                                                        }
                                                        if (!hasCollision) {
                                                            leftDragAccumulator -= stepPx
                                                            currentOnSavePlacement(widgetId, curItem.row, proposedStart, proposedSpan)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 2.dp, height = 16.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(MaterialTheme.colorScheme.onPrimary)
                                        )
                                    }
                                }

                                // 3. Right Edge Resize Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(width = 28.dp, height = 48.dp)
                                        .pointerInput(widgetId) {
                                            detectHorizontalDragGestures(
                                                onDragStart = { rightDragAccumulator = 0f },
                                                onDragEnd = { rightDragAccumulator = 0f },
                                                onDragCancel = { rightDragAccumulator = 0f }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                rightDragAccumulator += dragAmount
                                                val stepPx = (colWidthPx * 0.5f).coerceAtLeast(20.dp.toPx())
                                                val curItem = currentItem

                                                if (rightDragAccumulator >= stepPx) {
                                                    // Expand to the right
                                                    val proposedSpan = curItem.span + 1
                                                    if (curItem.startCol + proposedSpan <= 8) {
                                                        val hasCollision = currentRow.any { other ->
                                                            other.widgetId != widgetId &&
                                                            WidgetGridLayoutEngine.isOverlapping(curItem.startCol, proposedSpan, other.startCol, other.span)
                                                        }
                                                        if (!hasCollision) {
                                                            rightDragAccumulator -= stepPx
                                                            currentOnSavePlacement(widgetId, curItem.row, curItem.startCol, proposedSpan)
                                                        }
                                                    }
                                                } else if (rightDragAccumulator <= -stepPx) {
                                                    // Contract from the right
                                                    if (curItem.span > 1) {
                                                        val proposedSpan = curItem.span - 1
                                                        val hasCollision = currentRow.any { other ->
                                                            other.widgetId != widgetId &&
                                                            WidgetGridLayoutEngine.isOverlapping(curItem.startCol, proposedSpan, other.startCol, other.span)
                                                        }
                                                        if (!hasCollision) {
                                                            rightDragAccumulator += stepPx
                                                            currentOnSavePlacement(widgetId, curItem.row, curItem.startCol, proposedSpan)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 2.dp, height = 16.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(MaterialTheme.colorScheme.onPrimary)
                                        )
                                    }
                                }

                                // 4. Bottom Drag Handle for Height Adjustment (scaled width based on column span)
                                val bottomHandleWidthDp = with(LocalDensity.current) {
                                    (colWidthPx * 0.6f * itemSpan).toDp().coerceIn(28.dp, 64.dp)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 2.dp)
                                        .size(width = bottomHandleWidthDp + 16.dp, height = 28.dp)
                                        .pointerInput(widgetId) {
                                            detectVerticalDragGestures(
                                                onDragStart = {
                                                    isDraggingHeight = true
                                                    accumulatedHeightDp = (currentHeightDp ?: currentItem.providerInfo.minHeight.coerceAtLeast(80)).toFloat()
                                                },
                                                onDragEnd = {
                                                    isDraggingHeight = false
                                                    currentOnSaveHeight(widgetId, currentHeightDp)
                                                },
                                                onDragCancel = { isDraggingHeight = false }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                accumulatedHeightDp = (accumulatedHeightDp + (dragAmount / density)).coerceIn(60f, 800f)
                                                currentHeightDp = accumulatedHeightDp.toInt()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = bottomHandleWidthDp, height = 10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 14.dp, height = 2.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(MaterialTheme.colorScheme.onPrimary)
                                        )
                                    }
                                }

                                // 5. Action Bar (Height Badge, Move Up, Move Down, Reset, Configure, Remove)
                                val displayHeight = currentHeightDp ?: providerInfo.minHeight.coerceAtLeast(80)
                                val hasCustomPlacement = getWidgetGridPlacement(widgetId) != null

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Live Height Badge
                                    Text(
                                        text = "${displayHeight}dp",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    )

                                    // Move Up Button (if itemRow > 0)
                                    if (itemRow > 0) {
                                        IconButton(
                                            onClick = {
                                                val targetRow = itemRow - 1
                                                val targetRowItems = currentRows.getOrNull(targetRow) ?: emptyList()
                                                val bestSlot = WidgetGridLayoutEngine.findFirstAvailableSlot(
                                                    span = itemSpan,
                                                    existingItems = targetRowItems,
                                                    preferredStartCol = itemStartCol.coerceIn(0, 8 - itemSpan)
                                                ) ?: itemStartCol.coerceIn(0, 8 - itemSpan)
                                                currentOnSavePlacement(widgetId, targetRow, bestSlot, itemSpan)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Move Down Button
                                    IconButton(
                                        onClick = {
                                            val targetRow = itemRow + 1
                                            val targetRowItems = currentRows.getOrNull(targetRow) ?: emptyList()
                                            val bestSlot = WidgetGridLayoutEngine.findFirstAvailableSlot(
                                                span = itemSpan,
                                                existingItems = targetRowItems,
                                                preferredStartCol = itemStartCol.coerceIn(0, 8 - itemSpan)
                                            ) ?: itemStartCol.coerceIn(0, 8 - itemSpan)
                                            currentOnSavePlacement(widgetId, targetRow, bestSlot, itemSpan)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    // Reset Size Button
                                    if (currentHeightDp != null || hasCustomPlacement) {
                                        IconButton(
                                            onClick = {
                                                currentHeightDp = null
                                                currentOnSaveHeight(widgetId, null)
                                                currentOnResetPlacement(widgetId)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = "Reset Size",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Configure Button (if available)
                                    if (providerInfo.configure != null) {
                                        IconButton(
                                            onClick = { currentOnConfigureClick(widgetId, providerInfo) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Configure Widget",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    // Remove Button
                                    IconButton(
                                        onClick = { currentOnRemoveClick(widgetId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Widget",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        currentColumnIndex = item.startCol + item.span
                    }
                }

                // If row columns end < 8, insert trailing Spacer
                if (currentColumnIndex < 8) {
                    val trailingSpacerWeight = 8 - currentColumnIndex
                    Spacer(modifier = Modifier.weight(trailingSpacerWeight.toFloat()))
                }
            }
        }

        // Optional Trailing Controls (Add Widget & Done Buttons)
        if ((showAddWidgetButton || isContainerEditing) && (onAddWidgetClick != null || (isContainerEditing && onFinishEditing != null))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onAddWidgetClick != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable(onClick = onAddWidgetClick)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Widget",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Widget",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isContainerEditing && onFinishEditing != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .clickable(onClick = onFinishEditing)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

