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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
            val remainingApps = apps.drop(neutralCount)
            
            val categories = listOf("Communication", "Media", "Work", "Games", "System", "Utilities", "Uncategorized")
            
            val folders = mutableListOf<FolderState>()
            val appsPerFolder = if (categories.isNotEmpty()) remainingApps.size / categories.size else remainingApps.size
            
            categories.forEachIndexed { index, name ->
                val folderApps = if (index == categories.size - 1) {
                    remainingApps.drop(index * appsPerFolder)
                } else {
                    remainingApps.drop(index * appsPerFolder).take(appsPerFolder)
                }
                
                if (folderApps.isNotEmpty()) {
                    folders.add(
                        FolderState(
                            name = name,
                            cards = folderApps.map { 
                                CardState(
                    )
                }
                                ) 
                            }
                        )
                    )
                    folders = folders,
                    neutralApps = neutralApps,
                    isLoading = false
                )
            }
        }
    }

    fun updateCardSize(folderId: String, cardId: String, widthCells: Int, heightCells: Int) {
        _state.update { currentState ->
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(
                        cards = folder.cards.map { card ->
                            if (card.id == cardId) {
                                card.copy(widthCells = widthCells, heightCells = heightCells)
                            } else card
                        }
                    )
                } else folder
            }
            currentState.copy(folders = updatedFolders)
) {
    }

    fun setWidgetConfigured(
        folderId: String,
        cardId: String,
        appWidgetId: Int,
        widthCells: Int? = null,
        heightCells: Int? = null
    ) {
        _state.update { currentState ->
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(
                        cards = folder.cards.map { card ->
                            if (card.id == cardId) {
                                card.copy(
                                    isWidgetConfigured = true,
                                    appWidgetId = appWidgetId,
                                    widthCells = widthCells ?: card.widthCells,
                                    heightCells = heightCells ?: card.heightCells
                                )
                            } else card
                        }
                    )
                } else folder
            }
            currentState.copy(folders = updatedFolders)
        }
    }

    fun moveFromNeutralToFolder(app: AppModel) {
        _state.update { currentState ->
            val newNeutralApps = currentState.neutralApps.filter { it.packageName != app.packageName }
            
            // Find or create "Uncategorized" folder
            val uncategorizedName = "Uncategorized"
            var found = false
            val updatedFolders = currentState.folders.map { folder ->
                if (folder.name == uncategorizedName) {
                    found = true
                    folder.copy(cards = folder.cards + CardState(app = app))
                } else {
                    folder
                if (folder.name == uncategorizedName) {
                    found = true
                    folder.copy(cards = folder.cards + CardState(app = app, shortcuts = repository.getShortcuts(app)))
                } else {
                    folder
                }
                )
            }
            if (!found) {
                updatedFolders.add(
                    FolderState(name = uncategorizedName, cards = listOf(CardState(app = app, shortcuts = repository.getShortcuts(app))))
                )
            }
        }
    }

    fun launchApp(app: AppModel) {
        repository.launchApp(app)
    }
    
    fun getIcon(app: AppModel) = repository.getIcon(app)
}

            } else {
    
    fun getIcon(app: AppModel) = repository.getIcon(app)

    fun getShortcutIcon(shortcut: ShortcutInfo) = repository.getShortcutIcon(shortcut)
            pendingWidgetConfig = PendingWidgetConfig(folderId, cardId, appWidgetId, providerInfo)
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
        repository.launchShortcut(shortcut)
    }
}
            }

        }
    }

    if (state.isLoading) {

            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Canvas (Infinite Scroll)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp)
        ) {
            itemsIndexed(state.folders, key = { _, folder -> folder.id }) { index, folder ->
                FolderView(
                    folder = folder,
                    onCardClick = { viewModel.launchApp(it.app) },
                    onSizeChange = { cardId, newWidth, newHeight -> viewModel.updateCardSize(folder.id, cardId, newWidth, newHeight) },
                    onConfigureWidget = { card -> widgetPickerTarget = folder.id to card },
                    getIcon = { viewModel.getIcon(it) },
                    onConfigureWidget = { card -> widgetPickerTarget = folder.id to card },
                    getIcon = { viewModel.getIcon(it) },
                    getShortcutIcon = { viewModel.getShortcutIcon(it) },
                    onShortcutClick = { viewModel.launchShortcut(it) },
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager
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

    private fun isFuzzySubsequence(text: String, query: String): Boolean {
    private fun isFuzzySubsequence(text: String, query: String): Boolean {
        var textIndex = 0
                            color = Color.Gray,
        while (textIndex < text.length && queryIndex < query.length) {
                        )
                    }
                    if (activeFolderIndex < state.folders.size - 1) {
                        Text(
        }
        return queryIndex == query.length
    }

    fun launchApp(app: AppModel) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val componentName = ComponentName(app.packageName, app.className)
        }
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(app.packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
    }

    fun getShortcuts(app: AppModel): List<ShortcutInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
            launcherApps.getShortcuts(query, app.userHandle) ?: emptyList()
        } catch (e: SecurityException) {
        widgetPickerTarget?.let { (folderId, card) ->
            WidgetPickerDialog(
            emptyList()
        }
    }

    fun getShortcutIcon(shortcut: ShortcutInfo): Drawable? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
        val density = context.resources.displayMetrics.densityDpi
        return try {
            launcherApps.getShortcutIconDrawable(shortcut, density)
        } catch (e: SecurityException) {
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderView(
    folder: FolderState,
    onCardClick: (CardState) -> Unit,
    onSizeChange: (String, Int, Int) -> Unit,
    onConfigureWidget: (CardState) -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
    onConfigureWidget: (CardState) -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    getShortcutIcon: (ShortcutInfo) -> android.graphics.drawable.Drawable?,
    onShortcutClick: (ShortcutInfo) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
        cards = folder.cards
    ) { card ->
        CardItem(
            card = card,
            onClick = { onCardClick(card) },
            onSizeChange = { newWidth, newHeight -> onSizeChange(card.id, newWidth, newHeight) },
            onConfigureWidget = { onConfigureWidget(card) },
            getIcon = getIcon,
            appWidgetHost = appWidgetHost,
            appWidgetManager = appWidgetManager
        )
    }
            onConfigureWidget = { onConfigureWidget(card) },
            getIcon = getIcon,
            getShortcutIcon = getShortcutIcon,
            onShortcutClick = onShortcutClick,
            appWidgetHost = appWidgetHost,
            appWidgetManager = appWidgetManager
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
    Spacer(modifier = Modifier.height(48.dp)) // Spacing between folders
}
@Composable
fun CardItem(
    card: CardState,
    onClick: () -> Unit,
    onSizeChange: (Int, Int) -> Unit,
    onConfigureWidget: () -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var activeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    onConfigureWidget: () -> Unit,
    getIcon: (AppModel) -> android.graphics.drawable.Drawable?,
    getShortcutIcon: (ShortcutInfo) -> android.graphics.drawable.Drawable?,
    onShortcutClick: (ShortcutInfo) -> Unit,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
    val rowHeightPx = columnWidthPx * 1.3f

    val isLeft = activeCorner == ResizeCorner.TOP_START || activeCorner == ResizeCorner.BOTTOM_START
    val isTop = activeCorner == ResizeCorner.TOP_START || activeCorner == ResizeCorner.TOP_END

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                // dragX and dragY are the remainders of the drag, so adding them creates a smooth continuous stretch that perfectly offsets the discrete jumps in constraints when grid cells change.
                val newMaxWidth = (constraints.maxWidth + dragX.roundToInt()).coerceAtLeast(constraints.minWidth)
                val newMaxHeight = (constraints.maxHeight + dragY.roundToInt()).coerceAtLeast(constraints.minHeight)
                
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = newMaxWidth,
                        maxHeight = newMaxHeight
                    )
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = if (isLeft) -dragX.roundToInt() else 0
                    val y = if (isTop) -dragY.roundToInt() else 0
                    placeable.placeRelative(x, y)
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .clickable { onClick() }
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
                if (card.widthCells > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Mock quick actions
                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)))

                // Quick Actions
                if (card.widthCells > 1 && card.shortcuts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                if (card.heightCells > 1) {
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.shortcuts.take(4).forEach { shortcut ->
                            val shortcutIcon = remember(shortcut.id) {
                            .weight(1f)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.isWidgetConfigured && card.appWidgetId != null) {
                            val providerInfo = remember(card.appWidgetId) {
                                appWidgetManager.getAppWidgetInfo(card.appWidgetId)
                            }
                            if (providerInfo != null) {
                                AndroidView(
                                    factory = { context ->
                                        appWidgetHost.createView(context, card.appWidgetId, providerInfo)
                                    },
                                    update = { hostView ->
                                        hostView.setAppWidget(card.appWidgetId, providerInfo)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
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
                            }
                        }
                    }
                }
            Alignment.BottomEnd to ResizeCorner.BOTTOM_END
        )

        handles.forEach { (alignment, corner) ->
            Box(
                modifier = Modifier
                    .align(alignment)
                    .size(32.dp)
                    .pointerInput(card.id, corner) {
                        detectDragGestures(
                            onDragStart = {
                                activeCorner = corner
                            },
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

                                val widthStep = columnWidthPx
                                val heightStep = rowHeightPx

                                var newWidth = currentCard.widthCells
                                var newHeight = currentCard.heightCells

                                if (dragX > widthStep) { newWidth++; dragX -= widthStep }
                                else if (dragX < -widthStep) { newWidth--; dragX += widthStep }

                                if (dragY > heightStep) { newHeight++; dragY -= heightStep }
                                else if (dragY < -heightStep) { newHeight--; dragY += heightStep }

                                newWidth = newWidth.coerceIn(1, 4)
                                newHeight = newHeight.coerceAtLeast(1)

                                if (newWidth != currentCard.widthCells || newHeight != currentCard.heightCells) {
                                    onSizeChange(newWidth, newHeight)
                                }
                            }
                        )
                    }
            ) {
                // Visual indicator for drag handle
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .padding(4.dp)
                        .size(12.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                )
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
            if (providers.isEmpty()) {
                Text("No widgets available for this app.", color = Color.LightGray)
            } else {
                LazyColumn(
                    modifier = Modifier
                    } else {
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(providers) { providerInfo ->
                        val label = remember(providerInfo) {
                            providerInfo.loadLabel(context.packageManager)
                }
            }
        }
    }
}
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
