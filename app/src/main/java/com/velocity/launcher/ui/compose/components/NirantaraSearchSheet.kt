package com.velocity.launcher.ui.compose.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NirantaraSearchSheet(
    query: String,
    results: List<AppModel>,
    recentSearches: List<String>,
    frequentlyUsedApps: List<AppModel>,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onAppSelected: (AppModel) -> Unit,
    onWebSearchSelected: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val currentOnClose by rememberUpdatedState(onClose)

    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    val velocityThresholdPx = with(density) { 1000.dp.toPx() }
    val offsetY = remember { Animatable(0f) }

    val nestedScrollConnection = remember(dismissThresholdPx, velocityThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (offsetY.value > 0f && delta < 0f) {
                    val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
                    val consumed = newOffset - offsetY.value
                    coroutineScope.launch {
                        offsetY.snapTo(newOffset)
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f) {
                    keyboardController?.hide()
                    coroutineScope.launch {
                        offsetY.snapTo(offsetY.value + delta)
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value > 0f) {
                    if (offsetY.value >= dismissThresholdPx || available.y > velocityThresholdPx) {
                        currentOnClose()
                    } else {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (offsetY.value > 0f) {
                    if (offsetY.value >= dismissThresholdPx || available.y > velocityThresholdPx) {
                        currentOnClose()
                    } else {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val onBg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = offsetY.value }
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        keyboardController?.hide()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || offsetY.value > 0f) {
                            change.consume()
                            val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                            coroutineScope.launch {
                                offsetY.snapTo(newOffset)
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value >= dismissThresholdPx) {
                                currentOnClose()
                            } else {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                )
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.86f)
                    )
                )
            )
    ) {
        // Specular light highlight overlay at top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // reverseLayout = true:
            // items added FIRST in code -> appear at BOTTOM (closest to search bar)
            // items added LAST in code  -> appear at TOP (far from search bar)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp)
            ) {
                if (query.isBlank()) {
                    // Recent chips -- FIRST in code -> BOTTOM (closest to search bar)
                    if (recentSearches.isNotEmpty()) {
                        item(key = "recent_chips") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { searchItem ->
                                    val chipShape = RoundedCornerShape(8.dp)
                                    Surface(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .clickable { onQueryChange(searchItem) },
                                        color = onBg.copy(alpha = 0.09f),
                                        border = BorderStroke(0.8.dp, onBg.copy(alpha = 0.14f)),
                                        shape = chipShape
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                start = 12.dp, end = 6.dp,
                                                top = 6.dp, bottom = 6.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = onBg.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = searchItem,
                                                color = onBg,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onRemoveHistoryItem(searchItem) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = onBg.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Recent header -- SECOND in code -> just above chips
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 24.dp, end = 16.dp,
                                        top = 12.dp, bottom = 4.dp
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text(
                                        text = "Clear",
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Frequently Used -- LAST in code -> TOP (far from search bar, no label)
                    if (frequentlyUsedApps.isNotEmpty()) {
                        items(frequentlyUsedApps, key = { "freq_${it.id}" }) { app ->
                            SearchAppItem(
                                app = app,
                                getIconBitmap = getIconBitmap,
                                onClick = { onAppSelected(app) }
                            )
                        }
                    }
                } else {
                    // App results -- FIRST in code -> BOTTOM (closest to search bar)
                    if (results.isNotEmpty()) {
                        items(results, key = { "search_${it.id}" }) { app ->
                            SearchAppItem(
                                app = app,
                                getIconBitmap = getIconBitmap,
                                onClick = { onAppSelected(app) }
                            )
                        }
                    } else {
                        item(key = "no_results") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No apps found",
                                    color = onBg.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Web search -- LAST in code -> TOP (far, subtle plain text fallback)
                    item(key = "web_search") {
                        Text(
                            text = "\u2197 Search web for \"$query\"",
                            color = onBg.copy(alpha = 0.38f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWebSearchSelected(query) }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Subtle separator between scrollable results and fixed search bar
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
            )

            // Search bar -- bottom, thumb zone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button -- subtle glass tile
                val backButtonShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(backButtonShape)
                        .background(onBg.copy(alpha = 0.08f))
                        .border(
                            BorderStroke(0.5.dp, onBg.copy(alpha = 0.15f)),
                            shape = backButtonShape
                        )
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = onBg.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Pill search field -- glass styling
                val searchPillShape = RoundedCornerShape(50)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = onBg,
                        fontSize = 15.sp
                    ),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(searchPillShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            onBg.copy(alpha = 0.12f),
                                            onBg.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                onBg.copy(alpha = 0.22f),
                                                onBg.copy(alpha = 0.06f)
                                            )
                                        )
                                    ),
                                    shape = searchPillShape
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search apps\u2026",
                                        color = onBg.copy(alpha = 0.4f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (query.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onQueryChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = onBg.copy(alpha = 0.45f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchAppItem(
    app: AppModel,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val iconBitmap = remember(app.id) { getIconBitmap(app) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = app.label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
