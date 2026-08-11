package com.velocity.launcher.ui.compose

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.velocity.launcher.data.AppModel
import kotlin.math.roundToInt

private data class PendingWidgetConfig(
    val folderId: String,
    val cardId: String,
    val appWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val oldAppWidgetId: Int? = null
)


private enum class ResizeCorner {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}

private fun calculateWidgetCellSize(
    providerInfo: AppWidgetProviderInfo,
    cellWidthDp: Float,
    cellHeightDp: Float
): Pair<Int, Int> {
    val minWidthDp = providerInfo.minWidth.toFloat()
    val minHeightDp = providerInfo.minHeight.toFloat()
    val reqWidth = kotlin.math.ceil(minWidthDp / cellWidthDp).toInt().coerceIn(1, 4)
    val reqHeight = (kotlin.math.ceil(minHeightDp / cellHeightDp).toInt() + 1).coerceAtLeast(1)
    return reqWidth to reqHeight
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    var editingCardId by remember { mutableStateOf<String?>(null) }
    var widgetPickerTarget by remember { mutableStateOf<Pair<String, CardState>?>(null) }
    var pendingWidgetConfig by remember { mutableStateOf<PendingWidgetConfig?>(null) }

    val configuration = LocalConfiguration.current
    val cellWidthDp = (configuration.screenWidthDp - 32f) / 4f
    val cellHeightDp = cellWidthDp * 1.3f

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pending = pendingWidgetConfig
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                val (reqWidth, reqHeight) = calculateWidgetCellSize(pending.providerInfo, cellWidthDp, cellHeightDp)
                viewModel.setWidgetConfigured(pending.folderId, pending.cardId, pending.appWidgetId, reqWidth, reqHeight)
                pending.oldAppWidgetId?.let { appWidgetHost.deleteAppWidgetId(it) }
            } else {
                appWidgetHost.deleteAppWidgetId(pending.appWidgetId)
            }
            pendingWidgetConfig = null
        }

    }

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pending = pendingWidgetConfig
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                if (pending.providerInfo.configure != null) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = pending.providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pending.appWidgetId)
                    }
                    configureLauncher.launch(intent)
                } else {
                    val (reqWidth, reqHeight) = calculateWidgetCellSize(pending.providerInfo, cellWidthDp, cellHeightDp)
                    viewModel.setWidgetConfigured(pending.folderId, pending.cardId, pending.appWidgetId, reqWidth, reqHeight)
                    pending.oldAppWidgetId?.let { appWidgetHost.deleteAppWidgetId(it) }
                    pendingWidgetConfig = null
                }
            } else {
                appWidgetHost.deleteAppWidgetId(pending.appWidgetId)
                pendingWidgetConfig = null
            }
        }

    }

    val onWidgetSelected: (String, String, AppModel, AppWidgetProviderInfo, Int?) -> Unit = { folderId, cardId, app, providerInfo, oldAppWidgetId ->

        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            app.userHandle,
            providerInfo.provider,
            null
        )

        if (bindAllowed) {
            if (providerInfo.configure != null) {
                pendingWidgetConfig = PendingWidgetConfig(folderId, cardId, appWidgetId, providerInfo, oldAppWidgetId)

                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = providerInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                configureLauncher.launch(intent)
            } else {
                val (reqWidth, reqHeight) = calculateWidgetCellSize(providerInfo, cellWidthDp, cellHeightDp)
                viewModel.setWidgetConfigured(folderId, cardId, appWidgetId, reqWidth, reqHeight)
            }
        } else {
            pendingWidgetConfig = PendingWidgetConfig(folderId, cardId, appWidgetId, providerInfo, oldAppWidgetId)

            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, app.userHandle)
            }
            bindLauncher.launch(intent)
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Canvas (Infinite Scroll)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { editingCardId = null }) },
            contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp)
        ) {
            itemsIndexed(state.folders, key = { _, folder -> folder.id }) { index, folder ->
                FolderView(
                    folder = folder,
                    editingCardId = editingCardId,
                    onEditStart = { cardId -> editingCardId = cardId },
                    onEditStop = { editingCardId = null },
                    onCardClick = { card ->
                        if (editingCardId != null) {
                            editingCardId = null
                        } else {
                            viewModel.launchApp(card.app)
                        }
                    },
                    onSizeChange = { cardId, newWidth, newHeight -> viewModel.updateCardSize(folder.id, cardId, newWidth, newHeight) },
                    onConfigureWidget = { card -> widgetPickerTarget = folder.id to card },
                    getIcon = { viewModel.getIcon(it) },
                    getShortcutIcon = { viewModel.getShortcutIcon(it) },
                    onShortcutClick = { viewModel.launchShortcut(it) },
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager
                )
            }
        }

        // Active Folder Label
        val activeFolderIndex by remember {
            derivedStateOf { listState.firstVisibleItemIndex }
        }
        
        val isScrolling = listState.isScrollInProgress

        if (state.folders.isNotEmpty()) {
            val activeFolder = state.folders.getOrNull(activeFolderIndex)
            if (activeFolder != null) {
                Text(
                    text = activeFolder.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, top = 48.dp)
                )
            }
            
            // Show next/prev labels during scroll
            AnimatedVisibility(
                visible = isScrolling,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 48.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (activeFolderIndex > 0) {
                        Text(
                            text = state.folders[activeFolderIndex - 1].name,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    if (activeFolderIndex < state.folders.size - 1) {
                        Text(
                            text = state.folders[activeFolderIndex + 1].name,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Neutral Area (Entry point for new apps)
        if (state.neutralApps.isNotEmpty()) {
            NeutralArea(
                apps = state.neutralApps,
                onAppClick = { viewModel.moveFromNeutralToFolder(it) },
                getIcon = { viewModel.getIcon(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        widgetPickerTarget?.let { (folderId, card) ->
            WidgetPickerDialog(
                app = card.app,
                appWidgetManager = appWidgetManager,
                onDismissRequest = { widgetPickerTarget = null },
                onWidgetSelected = { providerInfo ->
                    val currentFolderId = folderId
                    val currentCardId = card.id
                    val currentApp = card.app
                    widgetPickerTarget = null
                    onWidgetSelected(currentFolderId, currentCardId, currentApp, providerInfo, card.appWidgetId)

                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderView(
    folder: FolderState,
    editingCardId: String?,
    onEditStart: (String) -> Unit,
    onEditStop: () -> Unit,
    onCardClick: (CardState) -> Unit,
    onSizeChange: (String, Int, Int) -> Unit,
    onConfigureWidget: (CardState) -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    getShortcutIcon: (ShortcutInfo) -> android.graphics.drawable.Drawable?,
    onShortcutClick: (ShortcutInfo) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    FolderGridLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cards = folder.cards
    ) { card ->
        CardItem(
            card = card,
            isEditing = editingCardId == card.id,
            onEditStart = { onEditStart(card.id) },
            onEditStop = onEditStop,
            onClick = { onCardClick(card) },
            onSizeChange = { newWidth, newHeight -> onSizeChange(card.id, newWidth, newHeight) },
            onConfigureWidget = { onConfigureWidget(card) },
            getIcon = getIcon,
            getShortcutIcon = getShortcutIcon,
            onShortcutClick = onShortcutClick,
            appWidgetHost = appWidgetHost,
            appWidgetManager = appWidgetManager
        )
    }
    
    Spacer(modifier = Modifier.height(48.dp)) // Spacing between folders
}

@Composable
fun FolderGridLayout(
    modifier: Modifier = Modifier,
    cards: List<CardState>,
    content: @Composable (CardState) -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            cards.forEach { card ->
                Box(propagateMinConstraints = true) {
                    content(card)
                }
            }
        }
    ) { measurables, constraints ->
        val columns = 4
        val columnWidth = constraints.maxWidth / columns
        val rowHeight = (columnWidth * 1.3f).roundToInt() // Aspect ratio roughly 3:4 for 1x1

        val matrix = mutableMapOf<Pair<Int, Int>, Boolean>()
        var maxRow = 0

        val placedItems = measurables.mapIndexed { index, measurable ->
            val card = cards[index]
            val colSpan = card.widthCells.coerceAtMost(columns)
            val rowSpan = card.heightCells

            // Find first available slot
            var r = 0
            var c = 0
            var found = false
            while (!found) {
                if (c + colSpan > columns) {
                    r++
                    c = 0
                    continue
                }
                var canFit = true
                for (ir in r until r + rowSpan) {
                    for (ic in c until c + colSpan) {
                        if (matrix[ir to ic] == true) canFit = false
                    }
                }
                if (canFit) {
                    found = true
                    for (ir in r until r + rowSpan) {
                        for (ic in c until c + colSpan) {
                            matrix[ir to ic] = true
                        }
                    }
                    maxRow = maxOf(maxRow, r + rowSpan)
                } else {
                    c++
                    if (c >= columns) {
                        r++
                        c = 0
                    }
                }
            }

            // Measure with exact size of spanned cells minus padding
            val itemWidth = (columnWidth * colSpan) - 16.dp.roundToPx()
            val itemHeight = (rowHeight * rowSpan) - 16.dp.roundToPx()
            val placeable = measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(itemWidth, itemHeight)
            )
            
            // Return placement coordinates
            Triple(placeable, c * columnWidth + 8.dp.roundToPx(), r * rowHeight + 8.dp.roundToPx())
        }

        val totalHeight = maxRow * rowHeight
        layout(constraints.maxWidth, totalHeight) {
            placedItems.forEach { (placeable, x, y) ->
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
fun CardItem(
    card: CardState,
    isEditing: Boolean,
    onEditStart: () -> Unit,
    onEditStop: () -> Unit,
    onClick: () -> Unit,
    onSizeChange: (Int, Int) -> Unit,
    onConfigureWidget: () -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    getShortcutIcon: (ShortcutInfo) -> android.graphics.drawable.Drawable?,
    onShortcutClick: (ShortcutInfo) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var activeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    val density = LocalDensity.current
    val currentCard by rememberUpdatedState(card)

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val columnWidthPx = with(density) { ((screenWidth - 32.dp) / 4).toPx() }
    val rowHeightPx = columnWidthPx * 1.3f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val newMaxWidth = (constraints.maxWidth + dragX.roundToInt()).coerceAtLeast(0)
                val newMaxHeight = (constraints.maxHeight + dragY.roundToInt()).coerceAtLeast(0)
                
                val placeable = measurable.measure(
                    androidx.compose.ui.unit.Constraints.fixed(newMaxWidth, newMaxHeight)
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(card.id, isEditing) {
                detectTapGestures(
                    onLongPress = { onEditStart() },
                    onTap = { 
                        if (isEditing) onEditStop() 
                        else onClick() 
                    }
                )
            }
    ) {
        // Content layer
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (card.widthCells == 1 && card.heightCells == 1) Arrangement.Center else Arrangement.Top
        ) {
            val icon = remember(card.app.packageName) {
                getIcon(card.app)?.toBitmap()?.asImageBitmap()
            }

            if (card.widthCells == 1 && card.heightCells == 1) {
                // Compact view
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = card.app.label,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Gray, RoundedCornerShape(8.dp))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.app.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                // Expanded view (Header: Icon + Label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = card.app.label,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Gray, RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = card.app.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Actions
                if (card.widthCells > 1 && card.shortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.shortcuts.take(4).forEach { shortcut ->
                            val shortcutIcon = remember(shortcut.id) {
                                try {
                                    getShortcutIcon(shortcut)?.toBitmap()?.asImageBitmap()
                                } catch (e: SecurityException) {
                                    null
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { onShortcutClick(shortcut) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (shortcutIcon != null) {
                                    Image(
                                        bitmap = shortcutIcon,
                                        contentDescription = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut",
                                        modifier = Modifier.size(24.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = (shortcut.shortLabel ?: shortcut.longLabel ?: "?").take(1).toString(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Widget Area
                if (card.heightCells > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.isWidgetConfigured && card.appWidgetId != null) {
                            val providerInfo = remember(card.appWidgetId) {
                                appWidgetManager.getAppWidgetInfo(card.appWidgetId)
                            }
                            if (providerInfo != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    key(card.appWidgetId) {
                                        AndroidView(
                                            factory = { context ->
                                                appWidgetHost.createView(context, card.appWidgetId, providerInfo)
                                            },
                                            update = { hostView ->
                                                hostView.setAppWidget(card.appWidgetId, providerInfo)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (isEditing) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable { onConfigureWidget() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(onClick = { onConfigureWidget() }) {
                                                Text("Replace Widget", fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Widget Unavailable", color = Color.White.copy(alpha = 0.5f))
                            }

                        } else {
                            OutlinedButton(onClick = { onConfigureWidget() }) {
                                Text("Configure Widget", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Drag handles for all four corners
        if (isEditing) {
            val handles = listOf(
                Alignment.TopStart to ResizeCorner.TOP_START,
                Alignment.TopEnd to ResizeCorner.TOP_END,
                Alignment.BottomStart to ResizeCorner.BOTTOM_START,
                Alignment.BottomEnd to ResizeCorner.BOTTOM_END
            )

            handles.forEach { (alignment, corner) ->
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .size(32.dp)
                        .pointerInput(card.id, corner) {
                            detectDragGestures(
                                onDragStart = { activeCorner = corner },
                                onDragEnd = {
                                    dragX = 0f
                                    dragY = 0f
                                    activeCorner = null
                                },
                                onDragCancel = {
                                    dragX = 0f
                                    dragY = 0f
                                    activeCorner = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x
                                    val dy = dragAmount.y

                                    val deltaW = when (corner) {
                                        ResizeCorner.BOTTOM_END, ResizeCorner.TOP_END -> dx
                                        ResizeCorner.BOTTOM_START, ResizeCorner.TOP_START -> -dx
                                    }
                                    val deltaH = when (corner) {
                                        ResizeCorner.BOTTOM_END, ResizeCorner.BOTTOM_START -> dy
                                        ResizeCorner.TOP_END, ResizeCorner.TOP_START -> -dy
                                    }

                                    dragX += deltaW
                                    dragY += deltaH

                                    var newWidth = currentCard.widthCells
                                    var newHeight = currentCard.heightCells
                                    var changed = false

                                    if (dragX > columnWidthPx && newWidth < 4) { newWidth++; dragX -= columnWidthPx; changed = true }
                                    else if (dragX < -columnWidthPx && newWidth > 1) { newWidth--; dragX += columnWidthPx; changed = true }

                                    if (dragY > rowHeightPx) { newHeight++; dragY -= rowHeightPx; changed = true }
                                    else if (dragY < -rowHeightPx && newHeight > 1) { newHeight--; dragY += rowHeightPx; changed = true }

                                    if (changed) {
                                        onSizeChange(newWidth, newHeight)
                                    }
                                }
                            )
                        }
                ) {
                    // Handle visual
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(16.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun NeutralArea(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp)
            .padding(bottom = 32.dp) // navigation bar padding
    ) {
        Text(
            text = "Unassigned Apps",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            apps.take(4).forEach { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAppClick(app) }
                ) {
                    val icon = remember(app.packageName) {
                        getIcon(app)?.toBitmap()?.asImageBitmap()
                    }
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = app.label,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Gray, RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerDialog(
    app: AppModel,
    appWidgetManager: AppWidgetManager,
    onDismissRequest: () -> Unit,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val providers = remember(app.packageName) {
        appWidgetManager.getInstalledProvidersForPackage(app.packageName, app.userHandle)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Widget for ${app.label}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (providers.isEmpty()) {
                Text("No widgets available for this app.", color = Color.LightGray)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(providers) { providerInfo ->
                        val label = remember(providerInfo) {
                            providerInfo.loadLabel(context.packageManager)
                        }
                        val iconDrawable = remember(providerInfo) {
                            try {
                                providerInfo.loadPreviewImage(context, 0)
                                    ?: providerInfo.loadIcon(context, context.resources.displayMetrics.densityDpi)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onWidgetSelected(providerInfo) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (iconDrawable != null) {
                                val bitmap = remember(iconDrawable) {
                                    try {
                                        iconDrawable.toBitmap().asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = label.toString(),
                                        modifier = Modifier.size(40.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.Gray, RoundedCornerShape(8.dp))
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Gray, RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label.toString(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${providerInfo.minWidth}dp x ${providerInfo.minHeight}dp",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
