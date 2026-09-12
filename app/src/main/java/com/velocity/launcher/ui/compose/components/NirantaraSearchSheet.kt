package com.velocity.launcher.ui.compose.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.ui.theme.calculateLuminance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SEARCH_BACKDROP_DARK = Color(0xFF0C0D14)
private val SEARCH_BACKDROP_LIGHT = Color(0xFFF8F9FD)
private const val SEARCH_BACKDROP_DARK_ALPHA = 0.88f
private const val SEARCH_BACKDROP_LIGHT_ALPHA = 0.94f
private const val SEARCH_BACKDROP_DARK_BOTTOM_ALPHA = 0.96f
private const val SEARCH_BACKDROP_LIGHT_BOTTOM_ALPHA = 0.98f

private val SEARCH_BAR_CORNER_RADIUS: Dp = 24.dp
private val SEARCH_BAR_HEIGHT: Dp = 48.dp
private val SEARCH_BACK_BUTTON_SIZE: Dp = 44.dp
private val SEARCH_BACK_BUTTON_CORNER_RADIUS: Dp = 20.dp
private val SEARCH_APP_ICON_SIZE: Dp = 44.dp
private val SEARCH_APP_ICON_CORNER: Dp = 10.dp
private val FREQUENT_APP_ICON_SIZE: Dp = 44.dp
private val FREQUENT_APP_ICON_CORNER: Dp = 10.dp
private val FREQUENT_ITEM_WIDTH: Dp = 76.dp
private val DISMISS_THRESHOLD_DP: Dp = 100.dp
private val VELOCITY_THRESHOLD_DP: Dp = 1000.dp

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

    val dismissThresholdPx = with(density) { DISMISS_THRESHOLD_DP.toPx() }
    val velocityThresholdPx = with(density) { VELOCITY_THRESHOLD_DP.toPx() }
    val offsetY = remember { Animatable(0f) }
    val listState = rememberLazyListState()

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
                if (delta > 0f && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
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
        delay(60)
        keyboardController?.show()
    }

    BackHandler(onBack = onClose)

    val onBg = MaterialTheme.colorScheme.onBackground
    val isDark = calculateLuminance(onBg) > 0.5f
    val backdropBase = if (isDark) SEARCH_BACKDROP_DARK else SEARCH_BACKDROP_LIGHT
    val backdropTopAlpha = if (isDark) SEARCH_BACKDROP_DARK_ALPHA else SEARCH_BACKDROP_LIGHT_ALPHA
    val backdropBottomAlpha = if (isDark) SEARCH_BACKDROP_DARK_BOTTOM_ALPHA else SEARCH_BACKDROP_LIGHT_BOTTOM_ALPHA

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = offsetY.value }
            .nestedScroll(nestedScrollConnection)
            .background(
                Brush.verticalGradient(
                    listOf(
                        backdropBase.copy(alpha = backdropTopAlpha),
                        backdropBase.copy(alpha = backdropBottomAlpha)
                    )
                )
            )
    ) {
        // Specular light highlight overlay at top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            (if (isDark) Color.White else Color.Black).copy(alpha = if (isDark) 0.06f else 0.03f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // Drag handle area for direct top swipe-down dismiss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { keyboardController?.hide() },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || offsetY.value > 0f) {
                                    change.consume()
                                    val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                                    coroutineScope.launch { offsetY.snapTo(newOffset) }
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(onBg.copy(alpha = 0.22f))
                )
            }

            // Natural top-to-bottom layout
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = false,
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                if (query.isBlank()) {
                    // Suggested / Frequently Used
                    if (frequentlyUsedApps.isNotEmpty()) {
                        item(key = "frequently_used_header") {
                            Text(
                                text = "Suggested",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        item(key = "frequently_used_grid") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                maxItemsInEachRow = 4
                            ) {
                                val displayApps = frequentlyUsedApps.take(8)
                                displayApps.forEach { app ->
                                    FrequentAppItem(
                                        app = app,
                                        getIconBitmap = getIconBitmap,
                                        onClick = { onAppSelected(app) }
                                    )
                                }
                            }
                        }
                    }

                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
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
                        item(key = "recent_chips") {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { searchItem ->
                                    val chipShape = RoundedCornerShape(12.dp)
                                    Surface(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .clickable { onQueryChange(searchItem) },
                                        color = onBg.copy(alpha = 0.08f),
                                        border = BorderStroke(0.8.dp, onBg.copy(alpha = 0.12f)),
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
                                                tint = onBg.copy(alpha = 0.6f),
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
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = onBg.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // App Results
                    if (results.isNotEmpty()) {
                        items(results, key = { "search_${it.id}" }) { app ->
                            SearchAppItem(
                                app = app,
                                query = query,
                                getIconBitmap = getIconBitmap,
                                onClick = { onAppSelected(app) }
                            )
                        }

                        // Web search card at the end of matching results
                        item(key = "web_search_card") {
                            WebSearchCard(
                                query = query,
                                onClick = { onWebSearchSelected(query) }
                            )
                        }
                    } else {
                        // Empty state
                        item(key = "no_results") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(onBg.copy(alpha = 0.06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = onBg.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No apps found for \"$query\"",
                                    color = onBg.copy(alpha = 0.7f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                WebSearchCard(
                                    query = query,
                                    onClick = { onWebSearchSelected(query) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = onBg.copy(alpha = 0.06f)
            )

            // Sleek bottom thumb-zone search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backShape = RoundedCornerShape(SEARCH_BACK_BUTTON_CORNER_RADIUS)
                Box(
                    modifier = Modifier
                        .size(SEARCH_BACK_BUTTON_SIZE)
                        .clip(backShape)
                        .background(onBg.copy(alpha = 0.08f))
                        .border(
                            BorderStroke(0.8.dp, onBg.copy(alpha = 0.14f)),
                            shape = backShape
                        )
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = onBg.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                val searchPillShape = RoundedCornerShape(SEARCH_BAR_CORNER_RADIUS)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = onBg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (results.isNotEmpty()) {
                                onAppSelected(results.first())
                            } else if (query.isNotBlank()) {
                                onWebSearchSelected(query)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SEARCH_BAR_HEIGHT)
                                .clip(searchPillShape)
                                .background(onBg.copy(alpha = 0.08f))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                onBg.copy(alpha = 0.20f),
                                                onBg.copy(alpha = 0.08f)
                                            )
                                        )
                                    ),
                                    shape = searchPillShape
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = onBg.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(onBg.copy(alpha = 0.1f))
                                        .clickable { onQueryChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = onBg.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
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
    query: String,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val iconBitmap = remember(app.id) { getIconBitmap(app) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val itemShape = RoundedCornerShape(12.dp)

    val highlightedLabel = remember(app.label, query, primaryColor, onBg) {
        buildHighlightedText(
            text = app.label,
            query = query,
            highlightColor = primaryColor,
            normalColor = onBg
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(itemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(SEARCH_APP_ICON_SIZE)
                    .clip(RoundedCornerShape(SEARCH_APP_ICON_CORNER))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(SEARCH_APP_ICON_SIZE)
                    .clip(RoundedCornerShape(SEARCH_APP_ICON_CORNER))
                    .background(onBg.copy(alpha = 0.12f))
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = highlightedLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (app.isWorkProfile) "Work • ${app.packageName}" else app.packageName,
                color = onBg.copy(alpha = 0.45f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FrequentAppItem(
    app: AppModel,
    getIconBitmap: (AppModel) -> ImageBitmap?,
    onClick: () -> Unit
) {
    val iconBitmap = remember(app.id) { getIconBitmap(app) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val itemShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .width(FREQUENT_ITEM_WIDTH)
            .clip(itemShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(FREQUENT_APP_ICON_SIZE)
                    .clip(RoundedCornerShape(FREQUENT_APP_ICON_CORNER))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(FREQUENT_APP_ICON_SIZE)
                    .clip(RoundedCornerShape(FREQUENT_APP_ICON_CORNER))
                    .background(onBg.copy(alpha = 0.12f))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = onBg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WebSearchCard(
    query: String,
    onClick: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
        color = primaryColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search web",
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "\"$query\"",
                    color = onBg.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = primaryColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color,
    normalColor: Color
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) {
        return AnnotatedString(text)
    }

    val lowerText = text.lowercase()
    val lowerQuery = trimmedQuery.lowercase()
    val matchIndex = lowerText.indexOf(lowerQuery)

    if (matchIndex < 0) {
        // Subsequence match fallback
        return buildAnnotatedString {
            var qIdx = 0
            for (char in text) {
                if (qIdx < lowerQuery.length && char.lowercaseChar() == lowerQuery[qIdx]) {
                    withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                        append(char)
                    }
                    qIdx++
                } else {
                    withStyle(SpanStyle(color = normalColor)) {
                        append(char)
                    }
                }
            }
        }
    }

    return buildAnnotatedString {
        var currentIndex = 0
        var searchFrom = 0
        while (searchFrom < text.length) {
            val idx = lowerText.indexOf(lowerQuery, searchFrom)
            if (idx == -1) {
                append(text.substring(currentIndex))
                break
            }
            if (idx > currentIndex) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(text.substring(currentIndex, idx))
                }
            }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(idx, idx + lowerQuery.length))
            }
            currentIndex = idx + lowerQuery.length
            searchFrom = currentIndex
        }
    }
}
