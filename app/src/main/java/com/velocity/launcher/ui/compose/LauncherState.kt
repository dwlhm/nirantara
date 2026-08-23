package com.velocity.launcher.ui.compose

import android.content.pm.ShortcutInfo
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppNotificationModel
import com.velocity.launcher.data.BackgroundType
import com.velocity.launcher.data.BatteryState
import com.velocity.launcher.data.ColorTone
import com.velocity.launcher.data.ScrollbarPosition
import com.velocity.launcher.data.ScrollbarVerticalAlignment
import com.velocity.launcher.data.SolidColorPreset
import com.velocity.launcher.data.ThemeMode
import com.velocity.launcher.data.TopSpacingMode

@Immutable
data class AppAlphabetSection(
    val letter: String,
    val apps: List<AppModel>
)

@Immutable
data class LauncherState(
    val allApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val alphabetSections: List<AppAlphabetSection> = emptyList(),
    val availableAlphabet: List<String> = emptyList(),
    val letterToScrollIndex: Map<String, Int> = emptyMap(),
    val hiddenApps: List<AppModel> = emptyList(),
    
    // Search
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<AppModel> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val frequentlyUsedApps: List<AppModel> = emptyList(),

    // Header & Battery
    val batteryState: BatteryState = BatteryState(),
    val topWidgetId: Int = -1,
    val topWidgetIds: List<Int> = emptyList(),
    val activeEditingContainerKey: String? = null,
    val widgetsRevision: Long = 0L,

    // Dialogs & Screens
    val activeBottomSheetApp: AppModel? = null,
    val activeBottomSheetShortcuts: List<ShortcutInfo> = emptyList(),
    val activePopupApp: AppModel? = null,
    val activePopupShortcuts: List<ShortcutInfo> = emptyList(),
    val activePopupNotifications: List<AppNotificationModel> = emptyList(),
    val hasShortcutHostPermission: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isWidgetPickerOpen: Boolean = false,
    val widgetPickerTargetApp: AppModel? = null,
    val isHiddenAppsOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val activeEditingLetter: String? = null,
    val customRadialPins: Map<String, List<String>> = emptyMap(),

    // Settings
    val scrollbarPosition: ScrollbarPosition = ScrollbarPosition.RIGHT,
    val scrollbarVerticalAlignment: ScrollbarVerticalAlignment = ScrollbarVerticalAlignment.BOTTOM,
    val backgroundType: BackgroundType = BackgroundType.WALLPAPER,
    val colorTone: ColorTone = ColorTone.ADAPTIVE,
    val themeMode: ThemeMode = ThemeMode.ADAPTIVE,
    val topSpacingMode: TopSpacingMode = TopSpacingMode.NORMAL,
    val hapticFeedbackEnabled: Boolean = true,
    val isLoading: Boolean = true,

    // Wallpaper & Custom Colors
    val customWallpaperBitmap: ImageBitmap? = null,
    val customWallpaperPath: String? = null,
    val extractedPaletteColors: List<Color> = emptyList(),
    val wallpaperColors: List<Color> = emptyList(),
    val adaptiveAccentColor: Color? = null,
    val wallpaperFilterOpacity: Float = 0.40f,
    val wallpaperFilterColor: Color = Color.Black,
    val solidColorPreset: SolidColorPreset = SolidColorPreset.AMOLED_BLACK,
    val solidBackgroundColor: Color = Color.Black,
    val solidAccentColor: Color = Color(0xFF818CF8)
)
