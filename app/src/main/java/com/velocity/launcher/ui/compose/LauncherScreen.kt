package com.velocity.launcher.ui.compose

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.data.TopSpacingMode
import com.velocity.launcher.ui.compose.components.AppListItem
import com.velocity.launcher.ui.compose.components.AppOptionsBottomSheet
import com.velocity.launcher.ui.compose.components.AppShortcutsPopup
import com.velocity.launcher.ui.compose.components.HiddenAppsScreen
import com.velocity.launcher.ui.compose.components.NirantaraHeader
import com.velocity.launcher.ui.compose.components.NirantaraSearchSheet
import com.velocity.launcher.ui.compose.components.NirantaraSettingsScreen
import com.velocity.launcher.ui.compose.components.QuickLaunchCustomizerSheet
import com.velocity.launcher.ui.compose.components.WaveAlphabetScrollbar
import com.velocity.launcher.ui.compose.components.WavePhysicsEngine
import com.velocity.launcher.ui.compose.components.WidgetPickerDialog
import com.velocity.launcher.ui.theme.SoftTextShadow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PendingWidgetConfig(
    val targetAppPackage: String?, // null if top header widget
    val appWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val isNew: Boolean = true
)

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeight = configuration.screenHeightDp.dp
    val focalOffsetDp = (screenHeight * 0.35f).coerceIn(160.dp, 320.dp)
    val focalOffsetPx = with(density) { focalOffsetDp.roundToPx() }

    val bottomOverscrollThresholdPx = with(density) { 48.dp.toPx() }
    var accumulatedBottomOverscroll by remember { mutableFloatStateOf(0f) }

    val bottomOverscrollConnection = remember(bottomOverscrollThresholdPx, state.isSearchOpen, state.isSettingsOpen) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0f) {
                    accumulatedBottomOverscroll += abs(available.y)
                    if (accumulatedBottomOverscroll >= bottomOverscrollThresholdPx) {
                        accumulatedBottomOverscroll = 0f
                        if (!state.isSearchOpen && !state.isSettingsOpen) {
                            viewModel.openSearch()
                        }
                    }
                } else if (available.y > 0f || consumed.y > 0f) {
                    accumulatedBottomOverscroll = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                accumulatedBottomOverscroll = 0f
                if (available.y < -1200f && !state.isSearchOpen && !state.isSettingsOpen) {
                    viewModel.openSearch()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    var isolatedLetter by remember { mutableStateOf<String?>(null) }
    var isolationJob by remember { mutableStateOf<Job?>(null) }

    BackHandler(enabled = state.activeEditingContainerKey != null) {
        viewModel.setActiveEditingContainer(null)
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            if (isolatedLetter != null) {
                isolatedLetter = null
                isolationJob?.cancel()
            }
        }
    }

    val topSpacerHeight = when (state.topSpacingMode) {
        TopSpacingMode.NORMAL -> (screenHeight * 0.22f).coerceIn(120.dp, 200.dp)
        TopSpacingMode.LARGE -> (screenHeight * 0.32f).coerceIn(180.dp, 300.dp)
        TopSpacingMode.COMPACT -> (screenHeight * 0.10f).coerceIn(50.dp, 100.dp)
        TopSpacingMode.NONE -> 0.dp
    }

    val scrollLookup = remember(state.availableAlphabet, state.letterToScrollIndex) {
        val sortedEntries = state.letterToScrollIndex.entries.sortedBy { it.value }
        val indices = IntArray(sortedEntries.size) { sortedEntries[it].value }
        val letters = Array(sortedEntries.size) { sortedEntries[it].key }
        Pair(indices, letters)
    }

    val currentLetters by remember(scrollLookup, state.availableAlphabet) {
        derivedStateOf {
            val (indices, letters) = scrollLookup
            val fallback = setOfNotNull(state.availableAlphabet.firstOrNull())
            if (indices.isEmpty()) {
                fallback
            } else {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                val firstVisible = visibleItems.firstOrNull()?.index ?: listState.firstVisibleItemIndex
                val lastVisible = visibleItems.lastOrNull()?.index ?: firstVisible

                WavePhysicsEngine.resolveVisibleLetters(
                    indices = indices,
                    letters = letters,
                    firstVisibleItemIndex = firstVisible,
                    lastVisibleItemIndex = lastVisible,
                    fallback = fallback
                )
            }
        }
    }

    val focusedLetter by remember(scrollLookup, state.availableAlphabet) {
        derivedStateOf {
            val (indices, letters) = scrollLookup
            val fallback = state.availableAlphabet.firstOrNull()
            if (indices.isEmpty()) {
                fallback
            } else {
                val firstVisible = listState.firstVisibleItemIndex
                WavePhysicsEngine.resolveLetterForScrollIndex(
                    indices = indices,
                    letters = letters,
                    firstVisibleItemIndex = firstVisible,
                    fallback = fallback
                )
            }
        }
    }

    var pendingWidgetConfig by remember { mutableStateOf<PendingWidgetConfig?>(null) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onWallpaperPicked(uri)
        }
    }

    // Activity result launcher for Widget Configuration
    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pending = pendingWidgetConfig
        if (pending != null) {
            if (result.resultCode == Activity.RESULT_OK) {
                if (pending.isNew) {
                    if (pending.targetAppPackage != null) {
                        viewModel.addAppPopupWidget(pending.targetAppPackage, pending.appWidgetId)
                    } else {
                        viewModel.addTopWidget(pending.appWidgetId)
                    }
                }
            } else {
                if (pending.isNew) {
                    appWidgetHost.deleteAppWidgetId(pending.appWidgetId)
                }
            }
            pendingWidgetConfig = null
        }
    }

    // Activity result launcher for Widget Binding Permission
    val bindWidgetLauncher = rememberLauncherForActivityResult(
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
                    configureWidgetLauncher.launch(intent)
                } else {
                    if (pending.isNew) {
                        if (pending.targetAppPackage != null) {
                            viewModel.addAppPopupWidget(pending.targetAppPackage, pending.appWidgetId)
                        } else {
                            viewModel.addTopWidget(pending.appWidgetId)
                        }
                    }
                    pendingWidgetConfig = null
                }
            } else {
                if (pending.isNew) {
                    appWidgetHost.deleteAppWidgetId(pending.appWidgetId)
                }
                pendingWidgetConfig = null
            }
        }
    }

    val handleWidgetSelected: (AppModel?, AppWidgetProviderInfo) -> Unit = { targetApp, providerInfo ->
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val userHandle = targetApp?.userHandle ?: android.os.Process.myUserHandle()
        val bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            userHandle,
            providerInfo.provider,
            null
        )

        if (bindAllowed) {
            if (providerInfo.configure != null) {
                pendingWidgetConfig = PendingWidgetConfig(targetApp?.packageName, appWidgetId, providerInfo, isNew = true)
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = providerInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                configureWidgetLauncher.launch(intent)
            } else {
                if (targetApp != null) {
                    viewModel.addAppPopupWidget(targetApp.packageName, appWidgetId)
                } else {
                    viewModel.addTopWidget(appWidgetId)
                }
            }
        } else {
            pendingWidgetConfig = PendingWidgetConfig(targetApp?.packageName, appWidgetId, providerInfo, isNew = true)
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, userHandle)
            }
            bindWidgetLauncher.launch(intent)
        }
    }

    val handleConfigureExistingWidget: (Int, AppWidgetProviderInfo) -> Unit = { widgetId, providerInfo ->
        if (providerInfo.configure != null) {
            pendingWidgetConfig = PendingWidgetConfig(null, widgetId, providerInfo, isNew = false)
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            configureWidgetLauncher.launch(intent)
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val wallpaper = state.customWallpaperBitmap

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Wallpaper / Background Base Layer
        when (state.backgroundType) {
            BackgroundType.SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(state.solidBackgroundColor)
                )
            }
            BackgroundType.WALLPAPER -> {
                if (wallpaper != null) {
                    androidx.compose.foundation.Image(
                        bitmap = wallpaper,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(state.wallpaperFilterColor.copy(alpha = state.wallpaperFilterOpacity))
                )
                when (state.colorTone) {
                    ColorTone.DARK -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                    }
                    ColorTone.LIGHT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.60f))
                        )
                    }
                    ColorTone.SYSTEM -> {
                        val scrimColor = if (isSystemDark) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.60f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scrimColor)
                        )
                    }
                    ColorTone.ADAPTIVE -> {
                        if (wallpaper != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.20f))
                            )
                        }
                    }
                }
            }
        }

        // Main Vertical Ergonomic Apps List
        LazyColumn(
            state = listState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(bottomOverscrollConnection)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = if (state.scrollbarPosition == ScrollbarPosition.LEFT || state.scrollbarPosition == ScrollbarPosition.BOTH) 40.dp else 4.dp,
                end = if (state.scrollbarPosition == ScrollbarPosition.RIGHT || state.scrollbarPosition == ScrollbarPosition.BOTH) 40.dp else 4.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            // 1. Home Area (Header and Favorites)
            item(key = "home_area") {
                val isHomeVisible = isolatedLetter == null || isolatedLetter == "★"
                val homeAlpha by animateFloatAsState(
                    targetValue = if (isHomeVisible) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "homeAlpha"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = homeAlpha }
                ) {
                    if (topSpacerHeight > 0.dp) {
                        Spacer(modifier = Modifier.height(topSpacerHeight))
                    }
                    NirantaraHeader(
                        batteryState = state.batteryState,
                        topWidgetIds = state.topWidgetIds,
                        topWidgetId = state.topWidgetId,
                        activeEditingContainerKey = state.activeEditingContainerKey,
                        widgetsRevision = state.widgetsRevision,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        onClockClick = { viewModel.openClock() },
                        onCalendarClick = { viewModel.openCalendar() },
                        onAddTopWidgetClick = { viewModel.openWidgetPicker(null) },
                        onRemoveTopWidgetClick = { widgetId -> viewModel.removeTopWidget(widgetId, appWidgetHost) },
                        onSetActiveEditingContainer = { key -> viewModel.setActiveEditingContainer(key) },
                        onConfigureWidgetClick = { widgetId, providerInfo -> handleConfigureExistingWidget(widgetId, providerInfo) },
                        getWidgetGridPlacement = viewModel::getWidgetGridPlacement,
                        onSaveWidgetGridPlacement = viewModel::saveWidgetGridPlacement,
                        onResetWidgetGridPlacement = viewModel::resetWidgetGridPlacement,
                        getWidgetCustomHeight = viewModel::getWidgetCustomHeight,
                        onSaveWidgetCustomHeight = viewModel::saveWidgetCustomHeight
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pinned / Favorite Apps
                    if (state.favoriteApps.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700).copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Favorites",
                                style = LocalTextStyle.current.copy(shadow = SoftTextShadow),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        state.favoriteApps.forEach { app ->
                            AppListItem(
                                app = app,
                                isFavoriteItem = true,
                                getIconBitmap = viewModel::getIconBitmap,
                                onClick = { viewModel.launchApp(app) },
                                onSwipeRight = { viewModel.openPopup(app) },
                                onSwipeLeftOrLongPress = { viewModel.openBottomSheet(app) },
                                activeEditingContainerKey = state.activeEditingContainerKey,
                                widgetsRevision = state.widgetsRevision,
                                onSetActiveEditingContainer = { key -> viewModel.setActiveEditingContainer(key) },
                                getWidgetGridPlacement = viewModel::getWidgetGridPlacement,
                                onSaveWidgetGridPlacement = viewModel::saveWidgetGridPlacement,
                                onResetWidgetGridPlacement = viewModel::resetWidgetGridPlacement,
                                getWidgetCustomHeight = viewModel::getWidgetCustomHeight,
                                onSaveWidgetCustomHeight = viewModel::saveWidgetCustomHeight,
                                onConfigureWidgetClick = { widgetId, providerInfo -> handleConfigureExistingWidget(widgetId, providerInfo) },
                                onRemoveWidgetClick = { pkg, widgetId -> viewModel.removeAppPopupWidget(pkg, widgetId, appWidgetHost) },
                                onAddWidgetClick = { targetApp -> viewModel.openWidgetPicker(targetApp) },
                                appWidgetHost = appWidgetHost,
                                appWidgetManager = appWidgetManager
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // 2. Alphabetical All Apps Drawer Sections
            state.alphabetSections.forEach { section ->
                item(key = "sec_header_${section.letter}") {
                    val isSectionVisible = isolatedLetter == null || isolatedLetter == section.letter
                    val sectionAlpha by animateFloatAsState(
                        targetValue = if (isSectionVisible) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "secAlpha_${section.letter}"
                    )

                    Text(
                        text = section.letter,
                        style = LocalTextStyle.current.copy(shadow = SoftTextShadow),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .graphicsLayer { alpha = sectionAlpha }
                            .padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                items(section.apps, key = { "app_${it.id}" }) { app ->
                    val isSectionVisible = isolatedLetter == null || isolatedLetter == section.letter
                    val sectionAlpha by animateFloatAsState(
                        targetValue = if (isSectionVisible) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "secAlpha_${section.letter}_${app.id}"
                    )

                    AppListItem(
                        app = app,
                        isFavoriteItem = false,
                        getIconBitmap = viewModel::getIconBitmap,
                        onClick = { viewModel.launchApp(app) },
                        onSwipeRight = { viewModel.openPopup(app) },
                        onSwipeLeftOrLongPress = { viewModel.openBottomSheet(app) },
                        activeEditingContainerKey = state.activeEditingContainerKey,
                        widgetsRevision = state.widgetsRevision,
                        onSetActiveEditingContainer = { key -> viewModel.setActiveEditingContainer(key) },
                        getWidgetGridPlacement = viewModel::getWidgetGridPlacement,
                        onSaveWidgetGridPlacement = viewModel::saveWidgetGridPlacement,
                        onResetWidgetGridPlacement = viewModel::resetWidgetGridPlacement,
                        getWidgetCustomHeight = viewModel::getWidgetCustomHeight,
                        onSaveWidgetCustomHeight = viewModel::saveWidgetCustomHeight,
                        onConfigureWidgetClick = { widgetId, providerInfo -> handleConfigureExistingWidget(widgetId, providerInfo) },
                        onRemoveWidgetClick = { pkg, widgetId -> viewModel.removeAppPopupWidget(pkg, widgetId, appWidgetHost) },
                        onAddWidgetClick = { targetApp -> viewModel.openWidgetPicker(targetApp) },
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        modifier = Modifier.graphicsLayer { alpha = sectionAlpha }
                    )
                }
            }

            item(key = "bottom_focal_spacer") {
                Spacer(modifier = Modifier.height(focalOffsetDp + 80.dp))
            }
        }

        // Wave Alphabet Scrollbar & Morphing Radial Arc (Top Layer)
        WaveAlphabetScrollbar(
            position = state.scrollbarPosition,
            alphabet = state.availableAlphabet,
            currentLetters = currentLetters,
            focusedLetter = focusedLetter,
            verticalAlignment = state.scrollbarVerticalAlignment,
            hapticEnabled = state.hapticFeedbackEnabled,
            onLetterSelected = { letter ->
                when (letter) {
                    "🔍" -> {
                        isolatedLetter = null
                        isolationJob?.cancel()
                        viewModel.openSearch()
                    }
                    "⚙" -> {
                        isolatedLetter = null
                        isolationJob?.cancel()
                        viewModel.openSettingsScreen()
                    }
                    else -> {
                        val targetIndex = viewModel.getScrollIndexForLetter(letter)
                        if (targetIndex != null) {
                            isolatedLetter = letter
                            isolationJob?.cancel()
                            isolationJob = coroutineScope.launch {
                                if (letter == "★") {
                                    listState.scrollToItem(0, 0)
                                } else {
                                    listState.scrollToItem(targetIndex, -focalOffsetPx)
                                }
                                delay(1500L)
                                isolatedLetter = null
                            }
                        }
                    }
                }
            },
            getRadialAppsForLetter = viewModel::getRadialAppsForLetter,
            getIconBitmap = viewModel::getIconBitmap,
            onLaunchApp = { app ->
                viewModel.launchApp(app)
            },
            onOpenRadialEditor = { letter ->
                viewModel.openRadialEditor(letter)
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        )

        // 4. Pop-up Shortcuts & Notifications (Swipe Right)
        AnimatedVisibility(
            visible = state.activePopupApp != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.activePopupApp?.let { app ->
                AppShortcutsPopup(
                    app = app,
                    shortcuts = state.activePopupShortcuts,
                    notifications = state.activePopupNotifications,
                    hasShortcutHostPermission = state.hasShortcutHostPermission,
                    isNotificationAccessGranted = state.isNotificationAccessGranted,
                    focalOffsetDp = focalOffsetDp,
                    getIconBitmap = viewModel::getIconBitmap,
                    getShortcutIconBitmap = viewModel::getShortcutIconBitmap,
                    onShortcutClick = viewModel::launchShortcut,
                    onNotificationClick = viewModel::openNotification,
                    onDismissNotification = viewModel::dismissNotification,
                    onRequestNotificationAccess = viewModel::requestNotificationAccess,
                    onOpenDefaultLauncherSettings = viewModel::openDefaultLauncherSettings,
                    onDismissRequest = viewModel::closePopup
                )
            }
        }

        // 5. App Options BottomSheet (Swipe Left / Long Press)
        state.activeBottomSheetApp?.let { app ->
            AppOptionsBottomSheet(
                app = app,
                shortcuts = state.activeBottomSheetShortcuts,
                hasShortcutHostPermission = state.hasShortcutHostPermission,
                getIconBitmap = viewModel::getIconBitmap,
                getShortcutIconBitmap = viewModel::getShortcutIconBitmap,
                onShortcutClick = viewModel::launchShortcut,
                onOpenDefaultLauncherSettings = {
                    viewModel.closeBottomSheet()
                    viewModel.openDefaultLauncherSettings()
                },
                onToggleFavorite = { viewModel.toggleFavorite(app) },
                onAttachOrChangeWidget = {
                    viewModel.closeBottomSheet()
                    viewModel.openWidgetPicker(app)
                },
                onRemoveWidget = {
                    app.popupWidgetIds.forEach { widgetId ->
                        viewModel.removeAppPopupWidget(app.packageName, widgetId, appWidgetHost)
                    }
                    viewModel.closeBottomSheet()
                },
                onAppInfoClick = { viewModel.openAppInfo(app) },
                onUninstallClick = { viewModel.uninstallApp(app) },
                onHideAppClick = { viewModel.toggleHideApp(app) },
                onDismissRequest = viewModel::closeBottomSheet
            )
        }

        // 6. Search Sheet (Learning History & Fuzzy Matching)
        AnimatedVisibility(
            visible = state.isSearchOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NirantaraSearchSheet(
                query = state.searchQuery,
                results = state.searchResults,
                recentSearches = state.recentSearches,
                frequentlyUsedApps = state.frequentlyUsedApps,
                getIconBitmap = viewModel::getIconBitmap,
                onQueryChange = viewModel::onSearchQueryChanged,
                onAppSelected = { app -> viewModel.launchApp(app, fromSearch = true) },
                onWebSearchSelected = viewModel::searchWeb,
                onRemoveHistoryItem = viewModel::removeSearchHistoryItem,
                onClearHistory = viewModel::clearSearchHistory,
                onClose = viewModel::closeSearch
            )
        }

        // 7. Hidden Apps Management Screen
        AnimatedVisibility(
            visible = state.isHiddenAppsOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HiddenAppsScreen(
                hiddenApps = state.hiddenApps,
                getIconBitmap = viewModel::getIconBitmap,
                onUnhideApp = viewModel::unhideApp,
                onClose = viewModel::closeHiddenAppsScreen
            )
        }

        // 8. Settings Screen
        AnimatedVisibility(
            visible = state.isSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NirantaraSettingsScreen(
                scrollbarPosition = state.scrollbarPosition,
                scrollbarVerticalAlignment = state.scrollbarVerticalAlignment,
                themeMode = state.themeMode,
                backgroundType = state.backgroundType,
                colorTone = state.colorTone,
                topSpacingMode = state.topSpacingMode,
                hapticFeedbackEnabled = state.hapticFeedbackEnabled,
                hiddenAppsCount = state.hiddenApps.size,
                topWidgetId = state.topWidgetId,
                customWallpaperBitmap = state.customWallpaperBitmap,
                wallpaperColors = state.wallpaperColors,
                adaptiveAccentColor = state.adaptiveAccentColor,
                solidColorPreset = state.solidColorPreset,
                solidBackgroundColor = state.solidBackgroundColor,
                solidAccentColor = state.solidAccentColor,
                wallpaperFilterOpacity = state.wallpaperFilterOpacity,
                wallpaperFilterColor = state.wallpaperFilterColor,
                onScrollbarPositionChange = viewModel::setScrollbarPosition,
                onScrollbarVerticalAlignmentChange = viewModel::setScrollbarVerticalAlignment,
                onThemeModeChange = viewModel::setThemeMode,
                onBackgroundTypeChange = viewModel::setBackgroundType,
                onColorToneChange = viewModel::setColorTone,
                onTopSpacingModeChange = viewModel::setTopSpacingMode,
                onHapticFeedbackChange = viewModel::setHapticFeedbackEnabled,
                onOpenHiddenApps = {
                    viewModel.closeSettingsScreen()
                    viewModel.openHiddenAppsScreen()
                },
                onClearSearchHistory = viewModel::clearSearchHistory,
                onAddTopWidgetClick = { viewModel.openWidgetPicker(null) },
                onRemoveTopWidgetClick = { viewModel.removeTopWidget() },
                onPickWallpaperClick = { wallpaperPickerLauncher.launch("image/*") },
                onRemoveWallpaperClick = viewModel::removeCustomWallpaper,
                onAdaptiveAccentColorChange = viewModel::setAdaptiveAccentColor,
                onWallpaperFilterOpacityChange = viewModel::setWallpaperFilterOpacity,
                onWallpaperFilterColorChange = viewModel::setWallpaperFilterColor,
                onSolidPresetChange = viewModel::setSolidPreset,
                onSolidBackgroundColorChange = viewModel::setSolidBackgroundColor,
                onSolidAccentColorChange = viewModel::setSolidAccentColor,
                onClose = viewModel::closeSettingsScreen
            )
        }

        // 9. Widget Picker Dialog
        if (state.isWidgetPickerOpen) {
            WidgetPickerDialog(
                targetApp = state.widgetPickerTargetApp,
                appWidgetManager = appWidgetManager,
                onDismissRequest = viewModel::closeWidgetPicker,
                onWidgetSelected = { providerInfo ->
                    val target = state.widgetPickerTargetApp
                    viewModel.closeWidgetPicker()
                    handleWidgetSelected(target, providerInfo)
                }
            )
        }

        // 10. In-Situ Quick-Launch Customizer Sheet
        state.activeEditingLetter?.let { letter ->
            QuickLaunchCustomizerSheet(
                letter = letter,
                currentPinnedApps = viewModel.getRadialAppsForLetter(letter),
                availableApps = viewModel.getAllAppsForLetter(letter),
                getIconBitmap = viewModel::getIconBitmap,
                onSave = { selectedApps ->
                    viewModel.saveRadialConfiguration(letter, selectedApps)
                },
                onResetToAuto = {
                    viewModel.resetRadialConfiguration(letter)
                },
                onDismissRequest = viewModel::closeRadialEditor
            )
        }
    }
}
