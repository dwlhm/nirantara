package com.velocity.launcher.ui.compose

import android.app.Application
import android.appwidget.AppWidgetHost
import android.content.pm.ShortcutInfo
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppNotificationModel
import com.velocity.launcher.data.AppRepository
import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.BatteryMonitor
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.LauncherNotificationListenerService
import com.velocity.launcher.data.PreferencesManager
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.data.SolidColorPreset
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.data.TopSpacingMode
import com.velocity.launcher.data.WallpaperColorExtractor
import com.velocity.launcher.data.WidgetGridPlacement
import com.velocity.launcher.ui.widget.WidgetSnapshotManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val preferencesManager = PreferencesManager(application)
    private val batteryMonitor = BatteryMonitor(application)

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    init {
        initBatteryMonitoring()
        initNotificationMonitoring()
        loadApps()
    }

    private fun initBatteryMonitoring() {
        viewModelScope.launch {
            batteryMonitor.batteryState.collectLatest { battery ->
                _state.update { it.copy(batteryState = battery) }
            }
        }
    }

    private fun initNotificationMonitoring() {
        viewModelScope.launch {
            LauncherNotificationListenerService.activeNotifications.collectLatest { map ->
                val currentPopupApp = _state.value.activePopupApp
                if (currentPopupApp != null) {
                    val updatedNotifications = map[currentPopupApp.packageName] ?: emptyList()
                    _state.update { it.copy(activePopupNotifications = updatedNotifications) }
                }
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_state.value.allApps.isEmpty()) {
                _state.update { it.copy(isLoading = true) }
            }
            val apps = repository.loadApps()

            val scrollbarPos = preferencesManager.scrollbarPosition
            val scrollbarVertAlign = preferencesManager.scrollbarVerticalAlignment
            val bgType = preferencesManager.backgroundType
            val colorTone = preferencesManager.colorTone
            val theme = preferencesManager.themeMode
            val topSpacing = preferencesManager.topSpacingMode
            val haptic = preferencesManager.hapticFeedbackEnabled
            val topWidget = preferencesManager.topWidgetId
            val topWidgetIds = preferencesManager.getTopWidgetIds()
            val history = preferencesManager.getSearchHistory()
            val launchCounts = preferencesManager.getSearchLaunchCounts()
            val savedWallpaperPath = preferencesManager.customWallpaperPath
            val savedAdaptiveAccentInt = preferencesManager.adaptiveAccentColor
            val solidPreset = preferencesManager.solidColorPreset
            val solidBgInt = preferencesManager.solidBackgroundColor
            val solidAccentInt = preferencesManager.solidAccentColor
            val filterOpacity = preferencesManager.wallpaperFilterOpacity
            val filterColorInt = preferencesManager.wallpaperFilterColor

            var wallpaperBitmap: ImageBitmap? = null
            var extractedColorsList: List<Color> = emptyList()
            var adaptiveAccent: Color? = savedAdaptiveAccentInt?.let { Color(it) }

            if (savedWallpaperPath != null) {
                val wallpaperFile = File(savedWallpaperPath)
                if (wallpaperFile.exists() && wallpaperFile.length() > 0) {
                    val bmp = WallpaperColorExtractor.loadWallpaperBitmap(wallpaperFile)
                    if (bmp != null) {
                        wallpaperBitmap = bmp.asImageBitmap()
                        val extracted = WallpaperColorExtractor.extractPalette(bmp)
                        extractedColorsList = extracted.swatchColors.map { Color(it) }
                        if (adaptiveAccent == null) {
                            adaptiveAccent = Color(extracted.primaryAccent)
                        }
                    }
                } else {
                    preferencesManager.clearCustomWallpaper()
                }
            }

            val hiddenApps = apps.filter { it.isHidden }
            val visibleApps = apps.filter { !it.isHidden }

            val favoriteApps = visibleApps.filter { it.isFavorite }
            val nonFavoriteApps = visibleApps.filter { !it.isFavorite }

            // Group non-favorites alphabetically
            val sectionsMap = mutableMapOf<String, MutableList<AppModel>>()
            for (app in nonFavoriteApps) {
                val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                val key = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
                sectionsMap.getOrPut(key) { mutableListOf() }.add(app)
            }

            val sortedKeys = sectionsMap.keys.sortedWith { a, b ->
                when {
                    a == "#" -> 1
                    b == "#" -> -1
                    else -> a.compareTo(b)
                }
            }

            val alphabetSections = sortedKeys.map { key ->
                AppAlphabetSection(
                    letter = key,
                    apps = sectionsMap[key]?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }) ?: emptyList()
                )
            }

            val availableAlphabet = buildList {
                add("★")
                for (sec in alphabetSections) {
                    add(sec.letter)
                }
                add("🔍")
                add("⚙")
            }

            // Calculate item index in the LazyColumn for fast wave-scrollbar jumping
            // Index 0: Home Area (key = "home_area")
            // Then each alphabet section has:
            //   1 Header item
            //   N App items
            val indexMap = mutableMapOf<String, Int>()
            indexMap["★"] = 0 // Home Area

            var currentIndex = 1 // First alphabet section starts at index 1

            for (sec in alphabetSections) {
                indexMap[sec.letter] = currentIndex
                currentIndex += 1 // Section header item
                currentIndex += sec.apps.size // Apps items
            }

            val freqApps = repository.filterAndRankApps(apps, "", launchCounts).take(8)

            val customPins = preferencesManager.getCustomRadialPins()

            _state.update { current ->
                val currentPopupPkg = current.activePopupApp?.packageName
                val updatedPopupApp = if (currentPopupPkg != null) {
                    apps.firstOrNull { it.packageName == currentPopupPkg } ?: current.activePopupApp
                } else null

                val currentSheetPkg = current.activeBottomSheetApp?.packageName
                val updatedSheetApp = if (currentSheetPkg != null) {
                    apps.firstOrNull { it.packageName == currentSheetPkg } ?: current.activeBottomSheetApp
                } else null

                current.copy(
                    activePopupApp = updatedPopupApp,
                    activeBottomSheetApp = updatedSheetApp,
                    allApps = visibleApps,
                    favoriteApps = favoriteApps,
                    alphabetSections = alphabetSections,
                    availableAlphabet = availableAlphabet,
                    letterToScrollIndex = indexMap,
                    hiddenApps = hiddenApps,
                    topWidgetId = topWidgetIds.firstOrNull() ?: topWidget,
                    topWidgetIds = topWidgetIds,
                    scrollbarPosition = scrollbarPos,
                    scrollbarVerticalAlignment = scrollbarVertAlign,
                    backgroundType = bgType,
                    colorTone = colorTone,
                    themeMode = theme,
                    topSpacingMode = topSpacing,
                    hapticFeedbackEnabled = haptic,
                    recentSearches = history,
                    frequentlyUsedApps = freqApps,
                    customWallpaperBitmap = wallpaperBitmap,
                    customWallpaperPath = if (wallpaperBitmap != null) savedWallpaperPath else null,
                    extractedPaletteColors = extractedColorsList,
                    wallpaperColors = extractedColorsList,
                    adaptiveAccentColor = adaptiveAccent,
                    solidColorPreset = solidPreset,
                    solidBackgroundColor = Color(solidBgInt),
                    solidAccentColor = Color(solidAccentInt),
                    wallpaperFilterOpacity = filterOpacity,
                    wallpaperFilterColor = Color(filterColorInt),
                    customRadialPins = customPins,
                    isLoading = false
                )
            }
        }
    }

    // Custom Wallpaper & Color Management
    fun onWallpaperPicked(uri: Uri) = setCustomWallpaper(uri)

    fun setCustomWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = WallpaperColorExtractor.copyUriToInternalStorage(getApplication(), uri)
            if (file != null && file.exists()) {
                val bitmap = WallpaperColorExtractor.loadWallpaperBitmap(file)
                if (bitmap != null) {
                    val extracted = WallpaperColorExtractor.extractPalette(bitmap)
                    val primaryAccent = extracted.primaryAccent
                    preferencesManager.setCustomWallpaper(file.absolutePath, primaryAccent)
                    preferencesManager.backgroundType = BackgroundType.WALLPAPER
                    val imageBitmap = bitmap.asImageBitmap()
                    val paletteColors = extracted.swatchColors.map { Color(it) }
                    val accentColor = Color(primaryAccent)

                    _state.update {
                        it.copy(
                            backgroundType = BackgroundType.WALLPAPER,
                            themeMode = preferencesManager.themeMode,
                            customWallpaperBitmap = imageBitmap,
                            customWallpaperPath = file.absolutePath,
                            extractedPaletteColors = paletteColors,
                            wallpaperColors = paletteColors,
                            adaptiveAccentColor = accentColor
                        )
                    }
                }
            }
        }
    }

    fun removeCustomWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            WallpaperColorExtractor.deleteCustomWallpaper(getApplication())
            preferencesManager.clearCustomWallpaper()
            _state.update {
                it.copy(
                    customWallpaperBitmap = null,
                    customWallpaperPath = null,
                    extractedPaletteColors = emptyList(),
                    wallpaperColors = emptyList(),
                    adaptiveAccentColor = null
                )
            }
        }
    }

    fun setAdaptiveAccentColor(color: Color?) {
        preferencesManager.adaptiveAccentColor = color?.toArgb()
        _state.update { it.copy(adaptiveAccentColor = color) }
    }

    fun setSolidPreset(preset: SolidColorPreset) = setSolidColorPreset(preset)

    fun setSolidColorPreset(preset: SolidColorPreset) {
        preferencesManager.solidColorPreset = preset
        if (preset != SolidColorPreset.CUSTOM) {
            val bg = preset.defaultBgColor
            val accent = preset.defaultAccentColor
            preferencesManager.solidBackgroundColor = bg
            preferencesManager.solidAccentColor = accent
            _state.update {
                it.copy(
                    solidColorPreset = preset,
                    solidBackgroundColor = Color(bg),
                    solidAccentColor = Color(accent)
                )
            }
        } else {
            _state.update { it.copy(solidColorPreset = preset) }
        }
    }

    fun setSolidBackgroundColor(color: Color) {
        val colorInt = color.toArgb()
        preferencesManager.solidBackgroundColor = colorInt
        preferencesManager.solidColorPreset = SolidColorPreset.CUSTOM
        _state.update {
            it.copy(
                solidBackgroundColor = color,
                solidColorPreset = SolidColorPreset.CUSTOM
            )
        }
    }

    fun setSolidAccentColor(color: Color) {
        val colorInt = color.toArgb()
        preferencesManager.solidAccentColor = colorInt
        preferencesManager.solidColorPreset = SolidColorPreset.CUSTOM
        _state.update {
            it.copy(
                solidAccentColor = color,
                solidColorPreset = SolidColorPreset.CUSTOM
            )
        }
    }

    fun setWallpaperFilterOpacity(opacity: Float) {
        preferencesManager.wallpaperFilterOpacity = opacity
        _state.update { it.copy(wallpaperFilterOpacity = opacity) }
    }

    fun setWallpaperFilterColor(color: Color) {
        val colorInt = color.toArgb()
        preferencesManager.wallpaperFilterColor = colorInt
        _state.update { it.copy(wallpaperFilterColor = color) }
    }

    // App Pinning / Favorite
    fun toggleFavorite(app: AppModel) {
        val isNowFav = preferencesManager.toggleFavoriteApp(app.packageName)
        loadApps()
        if (_state.value.activeBottomSheetApp?.packageName == app.packageName) {
            _state.update {
                it.copy(activeBottomSheetApp = it.activeBottomSheetApp?.copy(isFavorite = isNowFav))
            }
        }
    }

    // App Hiding
    fun toggleHideApp(app: AppModel) {
        val nextHidden = !app.isHidden
        preferencesManager.setAppHidden(app.packageName, nextHidden)
        loadApps()
        closeBottomSheet()
    }

    fun unhideApp(app: AppModel) {
        preferencesManager.setAppHidden(app.packageName, false)
        loadApps()
    }

    // Pop-up Widgets
    fun addAppPopupWidget(packageName: String, widgetId: Int) {
        preferencesManager.addPopupWidgetForApp(packageName, widgetId)
        val updatedIds = preferencesManager.getPopupWidgetsForApp(packageName)
        _state.update { current ->
            val updatedPopupApp = if (current.activePopupApp?.packageName == packageName) {
                current.activePopupApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activePopupApp
            }
            val updatedBottomSheetApp = if (current.activeBottomSheetApp?.packageName == packageName) {
                current.activeBottomSheetApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activeBottomSheetApp
            }
            current.copy(
                activePopupApp = updatedPopupApp,
                activeBottomSheetApp = updatedBottomSheetApp
            )
        }
        loadApps()
    }

    fun removeAppPopupWidget(packageName: String, widgetId: Int, appWidgetHost: AppWidgetHost? = null) {
        appWidgetHost?.deleteAppWidgetId(widgetId)
        WidgetSnapshotManager.deleteSnapshot(getApplication(), widgetId)
        preferencesManager.removePopupWidgetForApp(packageName, widgetId)
        preferencesManager.setWidgetCustomHeight(widgetId, null)
        preferencesManager.setWidgetCustomSpan(widgetId, null)
        preferencesManager.removeWidgetGridPlacement(widgetId)
        val updatedIds = preferencesManager.getPopupWidgetsForApp(packageName)
        _state.update { current ->
            val updatedPopupApp = if (current.activePopupApp?.packageName == packageName) {
                current.activePopupApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activePopupApp
            }
            val updatedBottomSheetApp = if (current.activeBottomSheetApp?.packageName == packageName) {
                current.activeBottomSheetApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activeBottomSheetApp
            }
            current.copy(
                activePopupApp = updatedPopupApp,
                activeBottomSheetApp = updatedBottomSheetApp,
                widgetsRevision = current.widgetsRevision + 1
            )
        }
        loadApps()
    }

    fun setAppPopupWidget(packageName: String, widgetId: Int?) {
        preferencesManager.setPopupWidgetForApp(packageName, widgetId)
        val updatedIds = preferencesManager.getPopupWidgetsForApp(packageName)
        _state.update { current ->
            val updatedPopupApp = if (current.activePopupApp?.packageName == packageName) {
                current.activePopupApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activePopupApp
            }
            val updatedBottomSheetApp = if (current.activeBottomSheetApp?.packageName == packageName) {
                current.activeBottomSheetApp.copy(popupWidgetIds = updatedIds)
            } else {
                current.activeBottomSheetApp
            }
            current.copy(
                activePopupApp = updatedPopupApp,
                activeBottomSheetApp = updatedBottomSheetApp,
                widgetsRevision = current.widgetsRevision + 1
            )
        }
        loadApps()
    }

    fun getWidgetCustomHeight(widgetId: Int): Int? = preferencesManager.getWidgetCustomHeight(widgetId)

    fun saveWidgetCustomHeight(widgetId: Int, heightDp: Int?) {
        preferencesManager.setWidgetCustomHeight(widgetId, heightDp)
        _state.update { it.copy(widgetsRevision = it.widgetsRevision + 1) }
    }

    // Top Header Widgets
    fun addTopWidget(widgetId: Int) {
        preferencesManager.addTopWidgetId(widgetId)
        val ids = preferencesManager.getTopWidgetIds()
        _state.update { it.copy(topWidgetIds = ids, topWidgetId = ids.firstOrNull() ?: -1, widgetsRevision = it.widgetsRevision + 1) }
    }

    fun removeTopWidget(widgetId: Int, appWidgetHost: AppWidgetHost? = null) {
        appWidgetHost?.deleteAppWidgetId(widgetId)
        WidgetSnapshotManager.deleteSnapshot(getApplication(), widgetId)
        preferencesManager.removeTopWidgetId(widgetId)
        preferencesManager.setWidgetCustomSpan(widgetId, null)
        preferencesManager.setWidgetCustomHeight(widgetId, null)
        preferencesManager.removeWidgetGridPlacement(widgetId)
        val ids = preferencesManager.getTopWidgetIds()
        _state.update { current ->
            current.copy(
                topWidgetIds = ids,
                topWidgetId = ids.firstOrNull() ?: -1,
                widgetsRevision = current.widgetsRevision + 1
            )
        }
    }

    fun setTopWidgetId(widgetId: Int) {
        if (widgetId == -1) {
            removeTopWidget()
        } else {
            addTopWidget(widgetId)
        }
    }

    fun removeTopWidget() {
        val first = _state.value.topWidgetIds.firstOrNull() ?: _state.value.topWidgetId
        if (first != -1) {
            removeTopWidget(first)
        } else {
            preferencesManager.topWidgetId = -1
            preferencesManager.setTopWidgetIds(emptyList())
            _state.update { it.copy(topWidgetId = -1, topWidgetIds = emptyList(), widgetsRevision = it.widgetsRevision + 1) }
        }
    }

    // Freeform 8-Column Grid Placement: row (0..), startCol (0..7), span (1..8)
    fun getWidgetGridPlacement(widgetId: Int): WidgetGridPlacement? = preferencesManager.getWidgetGridPlacement(widgetId)

    fun saveWidgetGridPlacement(widgetId: Int, row: Int, startCol: Int, span: Int) {
        val validRow = row.coerceAtLeast(0)
        val validStart = startCol.coerceIn(0, 7)
        val validSpan = span.coerceIn(1, 8 - validStart)
        preferencesManager.setWidgetGridPlacement(widgetId, validRow, validStart, validSpan)
        _state.update { it.copy(widgetsRevision = it.widgetsRevision + 1) }
    }

    fun resetWidgetGridPlacement(widgetId: Int) {
        preferencesManager.removeWidgetGridPlacement(widgetId)
        preferencesManager.setWidgetCustomSpan(widgetId, null)
        _state.update { it.copy(widgetsRevision = it.widgetsRevision + 1) }
    }

    // Widget Column Span (1..8) (legacy compatibility)
    fun getWidgetCustomSpan(widgetId: Int): Int? = preferencesManager.getWidgetCustomSpan(widgetId)

    fun saveWidgetCustomSpan(widgetId: Int, span: Int?) {
        preferencesManager.setWidgetCustomSpan(widgetId, span)
        if (span != null) {
            val validSpan = span.coerceIn(1, 8)
            preferencesManager.setWidgetGridPlacement(widgetId, 0, 0, validSpan)
        } else {
            preferencesManager.removeWidgetGridPlacement(widgetId)
        }
        _state.update { it.copy(widgetsRevision = it.widgetsRevision + 1) }
    }

    // Per-App Inline Widget Exposure
    fun toggleAppWidgetInlineExposure(packageName: String) {
        val isFav = preferencesManager.getFavoriteApps().contains(packageName)
        preferencesManager.toggleWidgetExposedInline(packageName, isFav)
        loadApps()
    }

    // Active In-Situ Editing Container
    fun setActiveEditingContainer(containerKey: String?) {
        _state.update { it.copy(activeEditingContainerKey = containerKey) }
    }

    // App Actions
    fun launchApp(app: AppModel, fromSearch: Boolean = false) {
        if (app.packageName == getApplication<Application>().packageName) {
            if (fromSearch) {
                preferencesManager.recordAppLaunchFromSearch(app.packageName)
                if (_state.value.searchQuery.isNotBlank()) {
                    preferencesManager.addSearchQuery(_state.value.searchQuery)
                }
                closeSearch()
            }
            openSettingsScreen()
            return
        }

        if (fromSearch) {
            preferencesManager.recordAppLaunchFromSearch(app.packageName)
            if (_state.value.searchQuery.isNotBlank()) {
                preferencesManager.addSearchQuery(_state.value.searchQuery)
            }
            closeSearch()
        }
        repository.launchApp(app)
    }

    fun getAllAppsForLetter(letter: String): List<AppModel> {
        if (letter == "★") {
            val favs = _state.value.favoriteApps
            return if (favs.isNotEmpty()) favs else _state.value.allApps
        }
        return _state.value.allApps.filter { app ->
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            key.equals(letter, ignoreCase = true)
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    fun launchShortcut(shortcut: ShortcutInfo) {
        closePopup()
        closeBottomSheet()
        repository.launchShortcut(shortcut)
    }

    fun openDefaultLauncherSettings() {
        com.velocity.launcher.util.DefaultLauncherHelper.openDefaultLauncherSettings(getApplication())
    }

    fun openAppInfo(app: AppModel) {
        closeBottomSheet()
        repository.openAppInfo(app)
    }

    fun uninstallApp(app: AppModel) {
        closeBottomSheet()
        repository.uninstallApp(app)
    }

    fun searchWeb(query: String) {
        preferencesManager.addSearchQuery(query)
        closeSearch()
        repository.searchWeb(query)
    }

    fun openClock() {
        repository.openClock()
    }

    fun openCalendar() {
        repository.openCalendar()
    }

    fun getIcon(app: AppModel) = repository.getIcon(app)

    fun getIconBitmap(app: AppModel, sizePx: Int = 128): ImageBitmap? = repository.getIconBitmap(app, sizePx)

    fun getShortcutIcon(shortcut: ShortcutInfo) = repository.getShortcutIcon(shortcut)

    fun getShortcutIconBitmap(shortcut: ShortcutInfo, sizePx: Int = 96): ImageBitmap? = repository.getShortcutIconBitmap(shortcut, sizePx)

    // Search
    fun openSearch() {
        val launchCounts = preferencesManager.getSearchLaunchCounts()
        val history = preferencesManager.getSearchHistory()
        val freq = repository.filterAndRankApps(_state.value.allApps, "", launchCounts).take(8)

        _state.update {
            it.copy(
                isSearchOpen = true,
                searchQuery = "",
                searchResults = emptyList(),
                recentSearches = history,
                frequentlyUsedApps = freq
            )
        }
    }

    fun closeSearch() {
        _state.update { it.copy(isSearchOpen = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun onSearchQueryChanged(query: String) {
        val launchCounts = preferencesManager.getSearchLaunchCounts()
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            repository.filterAndRankApps(_state.value.allApps, query, launchCounts)
        }

        _state.update {
            it.copy(
                searchQuery = query,
                searchResults = results
            )
        }
    }

    fun removeSearchHistoryItem(query: String) {
        preferencesManager.removeSearchQuery(query)
        _state.update { it.copy(recentSearches = preferencesManager.getSearchHistory()) }
    }

    fun clearSearchHistory() {
        preferencesManager.clearSearchHistory()
        _state.update { it.copy(recentSearches = emptyList()) }
    }

    // Bottom Sheet
    fun openBottomSheet(app: AppModel) {
        val hasPermission = repository.hasShortcutHostPermission()
        val shortcuts = if (hasPermission) repository.getShortcuts(app) else emptyList()
        _state.update {
            it.copy(
                activeBottomSheetApp = app,
                activeBottomSheetShortcuts = shortcuts,
                hasShortcutHostPermission = hasPermission
            )
        }
    }

    fun closeBottomSheet() {
        _state.update {
            it.copy(
                activeBottomSheetApp = null,
                activeBottomSheetShortcuts = emptyList()
            )
        }
    }

    // Pop-up Drawer (Swipe Right)
    fun openPopup(app: AppModel) {
        val hasPermission = repository.hasShortcutHostPermission()
        val shortcuts = if (hasPermission) repository.getShortcuts(app) else emptyList()
        val isGranted = LauncherNotificationListenerService.isNotificationAccessGranted(getApplication())
        val notifications = LauncherNotificationListenerService.activeNotifications.value[app.packageName] ?: emptyList()
        _state.update {
            it.copy(
                activePopupApp = app,
                activePopupShortcuts = shortcuts,
                activePopupNotifications = notifications,
                hasShortcutHostPermission = hasPermission,
                isNotificationAccessGranted = isGranted
            )
        }
    }

    fun closePopup() {
        _state.update {
            it.copy(
                activePopupApp = null,
                activePopupShortcuts = emptyList(),
                activePopupNotifications = emptyList()
            )
        }
    }

    fun openNotification(notification: AppNotificationModel) {
        try {
            notification.contentIntent?.send()
        } catch (e: Exception) {
            // Ignore intent send failure
        }
        closePopup()
    }

    fun dismissNotification(notification: AppNotificationModel) {
        LauncherNotificationListenerService.dismissNotification(notification.key)
        _state.update { current ->
            current.copy(
                activePopupNotifications = current.activePopupNotifications.filterNot { it.key == notification.key }
            )
        }
    }

    fun requestNotificationAccess() {
        LauncherNotificationListenerService.openNotificationAccessSettings(getApplication())
    }

    // Widget Picker
    fun openWidgetPicker(targetApp: AppModel?) {
        _state.update { it.copy(isWidgetPickerOpen = true, widgetPickerTargetApp = targetApp) }
    }

    fun closeWidgetPicker() {
        _state.update { it.copy(isWidgetPickerOpen = false, widgetPickerTargetApp = null) }
    }

    // Hidden Apps Screen
    fun openHiddenAppsScreen() {
        _state.update { it.copy(isHiddenAppsOpen = true) }
    }

    fun closeHiddenAppsScreen() {
        _state.update { it.copy(isHiddenAppsOpen = false) }
    }

    // Settings Screen
    fun openSettingsScreen() {
        _state.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettingsScreen() {
        _state.update { it.copy(isSettingsOpen = false) }
    }

    fun setScrollbarPosition(pos: ScrollbarPosition) {
        preferencesManager.scrollbarPosition = pos
        _state.update { it.copy(scrollbarPosition = pos) }
    }

    fun setScrollbarVerticalAlignment(alignment: ScrollbarVerticalAlignment) {
        preferencesManager.scrollbarVerticalAlignment = alignment
        _state.update { it.copy(scrollbarVerticalAlignment = alignment) }
    }

    fun setBackgroundType(type: BackgroundType) {
        preferencesManager.backgroundType = type
        _state.update {
            it.copy(
                backgroundType = type,
                themeMode = preferencesManager.themeMode
            )
        }
    }

    fun setColorTone(tone: ColorTone) {
        preferencesManager.colorTone = tone
        _state.update {
            it.copy(
                colorTone = tone,
                themeMode = preferencesManager.themeMode
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesManager.themeMode = mode
        _state.update {
            it.copy(
                themeMode = mode,
                backgroundType = preferencesManager.backgroundType,
                colorTone = preferencesManager.colorTone
            )
        }
    }

    fun setTopSpacingMode(mode: TopSpacingMode) {
        preferencesManager.topSpacingMode = mode
        _state.update { it.copy(topSpacingMode = mode) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        preferencesManager.hapticFeedbackEnabled = enabled
        _state.update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    fun getScrollIndexForLetter(letter: String): Int? {
        if (letter == "⚙" || letter == "🔍") return null
        val map = _state.value.letterToScrollIndex
        map[letter]?.let { return it }

        val alphabet = listOf("★") + ('A'..'Z').map { it.toString() } + listOf("#")
        val letterIdx = alphabet.indexOf(letter)
        if (letterIdx == -1) return null

        // Search forward for closest next available section
        for (i in (letterIdx + 1)..alphabet.lastIndex) {
            val next = map[alphabet[i]]
            if (next != null) return next
        }

        // Search backward for closest previous available section
        for (i in (letterIdx - 1) downTo 0) {
            val prev = map[alphabet[i]]
            if (prev != null) return prev
        }

        return null
    }
}
