package com.velocity.launcher.data

import android.content.Context
import android.content.SharedPreferences

enum class ScrollbarPosition {
    RIGHT,
    LEFT,
    BOTH
}

enum class ScrollbarVerticalAlignment {
    BOTTOM,
    CENTER,
    TOP
}

enum class BackgroundType {
    WALLPAPER,
    SOLID
}

enum class ColorTone {
    ADAPTIVE,
    DARK,
    LIGHT,
    SYSTEM
}

enum class ThemeMode {
    ADAPTIVE,
    DARK,
    LIGHT,
    SYSTEM,
    SOLID_COLOR
}

enum class SolidColorPreset(val displayName: String, val defaultBgColor: Int, val defaultAccentColor: Int) {
    AMOLED_BLACK("AMOLED Black", 0xFF000000.toInt(), 0xFF818CF8.toInt()),
    DEEP_CHARCOAL("Deep Charcoal", 0xFF121212.toInt(), 0xFF60A5FA.toInt()),
    MIDNIGHT_SLATE("Midnight Slate", 0xFF1E1E2E.toInt(), 0xFF38BDF8.toInt()),
    CLEAN_LIGHT("Clean Light", 0xFFF8F9FA.toInt(), 0xFF4F46E5.toInt()),
    CUSTOM("Kustom", 0xFF000000.toInt(), 0xFF818CF8.toInt());

    val title: String get() = displayName
}

enum class TopSpacingMode {
    NORMAL,
    LARGE,
    COMPACT,
    NONE
}

data class WidgetGridPlacement(
    val row: Int = 0,
    val startCol: Int = 0,
    val span: Int = 8
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "nirantara_launcher_prefs"
        private const val KEY_FAVORITE_APPS = "favorite_apps"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BACKGROUND_TYPE = "background_type"
        private const val KEY_COLOR_TONE = "color_tone"
        private const val KEY_SCROLLBAR_POSITION = "scrollbar_position"
        private const val KEY_SCROLLBAR_VERTICAL_ALIGNMENT = "scrollbar_vertical_alignment"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_TOP_SPACING_MODE = "top_spacing_mode"
        private const val KEY_TOP_WIDGET_ID = "top_widget_id"
        private const val KEY_TOP_WIDGET_IDS = "top_widget_ids"
        private const val KEY_WIDGET_CUSTOM_SPAN_PREFIX = "widget_span_"
        private const val KEY_INLINE_EXPOSED_APPS = "inline_exposed_apps"
        private const val KEY_INLINE_NON_EXPOSED_APPS = "inline_non_exposed_apps"
        private const val KEY_POPUP_WIDGETS = "popup_widgets"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_SEARCH_LAUNCH_COUNTS = "search_launch_counts"
        private const val KEY_CUSTOM_WALLPAPER_PATH = "custom_wallpaper_path"
        private const val KEY_ADAPTIVE_ACCENT_COLOR = "adaptive_accent_color"
        private const val KEY_SOLID_COLOR_PRESET = "solid_color_preset"
        private const val KEY_SOLID_BACKGROUND_COLOR = "solid_background_color"
        private const val KEY_SOLID_ACCENT_COLOR = "solid_accent_color"
        private const val KEY_CUSTOM_RADIAL_PINS = "custom_radial_pins"
        private const val KEY_WALLPAPER_FILTER_OPACITY = "wallpaper_filter_opacity"
        private const val KEY_WALLPAPER_FILTER_COLOR = "wallpaper_filter_color"
        private const val KEY_WIDGET_CUSTOM_HEIGHT_PREFIX = "widget_height_"
        private const val KEY_WIDGET_GRID_PREFIX = "widget_grid_"
    }

    // Scrollbar Position
    var scrollbarPosition: ScrollbarPosition
        get() {
            val name = prefs.getString(KEY_SCROLLBAR_POSITION, ScrollbarPosition.RIGHT.name)
            return try {
                ScrollbarPosition.valueOf(name ?: ScrollbarPosition.RIGHT.name)
            } catch (e: Exception) {
                ScrollbarPosition.RIGHT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SCROLLBAR_POSITION, value.name).apply()
        }

    // Scrollbar Vertical Alignment
    var scrollbarVerticalAlignment: ScrollbarVerticalAlignment
        get() {
            val name = prefs.getString(KEY_SCROLLBAR_VERTICAL_ALIGNMENT, ScrollbarVerticalAlignment.BOTTOM.name)
            return try {
                ScrollbarVerticalAlignment.valueOf(name ?: ScrollbarVerticalAlignment.BOTTOM.name)
            } catch (e: Exception) {
                ScrollbarVerticalAlignment.BOTTOM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SCROLLBAR_VERTICAL_ALIGNMENT, value.name).apply()
        }

    // Background Type
    var backgroundType: BackgroundType
        get() {
            if (prefs.contains(KEY_BACKGROUND_TYPE)) {
                val name = prefs.getString(KEY_BACKGROUND_TYPE, BackgroundType.WALLPAPER.name)
                return try {
                    BackgroundType.valueOf(name ?: BackgroundType.WALLPAPER.name)
                } catch (e: Exception) {
                    BackgroundType.WALLPAPER
                }
            }
            // Compatibility fallback from legacy KEY_THEME_MODE
            val legacyTheme = prefs.getString(KEY_THEME_MODE, null)
            return if (legacyTheme == "SOLID_COLOR" || legacyTheme == "AMOLED_BLACK") {
                BackgroundType.SOLID
            } else {
                BackgroundType.WALLPAPER
            }
        }
        set(value) {
            prefs.edit().putString(KEY_BACKGROUND_TYPE, value.name).apply()
        }

    // Color Tone
    var colorTone: ColorTone
        get() {
            if (prefs.contains(KEY_COLOR_TONE)) {
                val name = prefs.getString(KEY_COLOR_TONE, ColorTone.ADAPTIVE.name)
                return try {
                    ColorTone.valueOf(name ?: ColorTone.ADAPTIVE.name)
                } catch (e: Exception) {
                    ColorTone.ADAPTIVE
                }
            }
            // Compatibility fallback from legacy KEY_THEME_MODE
            val legacyTheme = prefs.getString(KEY_THEME_MODE, null)
            return when (legacyTheme) {
                "DARK" -> ColorTone.DARK
                "LIGHT" -> ColorTone.LIGHT
                "SYSTEM" -> ColorTone.SYSTEM
                else -> ColorTone.ADAPTIVE
            }
        }
        set(value) {
            prefs.edit().putString(KEY_COLOR_TONE, value.name).apply()
        }

    // Theme Mode (Compatibility mapping with BackgroundType & ColorTone)
    var themeMode: ThemeMode
        get() {
            return if (backgroundType == BackgroundType.SOLID) {
                ThemeMode.SOLID_COLOR
            } else {
                when (colorTone) {
                    ColorTone.ADAPTIVE -> ThemeMode.ADAPTIVE
                    ColorTone.DARK -> ThemeMode.DARK
                    ColorTone.LIGHT -> ThemeMode.LIGHT
                    ColorTone.SYSTEM -> ThemeMode.SYSTEM
                }
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
            when (value) {
                ThemeMode.SOLID_COLOR -> {
                    backgroundType = BackgroundType.SOLID
                }
                ThemeMode.ADAPTIVE -> {
                    backgroundType = BackgroundType.WALLPAPER
                    colorTone = ColorTone.ADAPTIVE
                }
                ThemeMode.DARK -> {
                    backgroundType = BackgroundType.WALLPAPER
                    colorTone = ColorTone.DARK
                }
                ThemeMode.LIGHT -> {
                    backgroundType = BackgroundType.WALLPAPER
                    colorTone = ColorTone.LIGHT
                }
                ThemeMode.SYSTEM -> {
                    backgroundType = BackgroundType.WALLPAPER
                    colorTone = ColorTone.SYSTEM
                }
            }
        }

    // Custom Wallpaper Path
    var customWallpaperPath: String?
        get() = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, value).apply()
            } else {
                prefs.edit().remove(KEY_CUSTOM_WALLPAPER_PATH).apply()
            }
        }

    // Adaptive Accent Color
    var adaptiveAccentColor: Int?
        get() = if (prefs.contains(KEY_ADAPTIVE_ACCENT_COLOR)) prefs.getInt(KEY_ADAPTIVE_ACCENT_COLOR, 0) else null
        set(value) {
            if (value != null) {
                prefs.edit().putInt(KEY_ADAPTIVE_ACCENT_COLOR, value).apply()
            } else {
                prefs.edit().remove(KEY_ADAPTIVE_ACCENT_COLOR).apply()
            }
        }

    // Solid Color Preset
    var solidColorPreset: SolidColorPreset
        get() {
            val name = prefs.getString(KEY_SOLID_COLOR_PRESET, SolidColorPreset.AMOLED_BLACK.name)
            return try {
                SolidColorPreset.valueOf(name ?: SolidColorPreset.AMOLED_BLACK.name)
            } catch (e: Exception) {
                SolidColorPreset.AMOLED_BLACK
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SOLID_COLOR_PRESET, value.name).apply()
        }

    // Solid Background Color
    var solidBackgroundColor: Int
        get() = prefs.getInt(KEY_SOLID_BACKGROUND_COLOR, 0xFF000000.toInt())
        set(value) {
            prefs.edit().putInt(KEY_SOLID_BACKGROUND_COLOR, value).apply()
        }

    // Solid Accent Color
    var solidAccentColor: Int
        get() = prefs.getInt(KEY_SOLID_ACCENT_COLOR, 0xFF818CF8.toInt())
        set(value) {
            prefs.edit().putInt(KEY_SOLID_ACCENT_COLOR, value).apply()
        }

    // Wallpaper Filter Layer
    var wallpaperFilterOpacity: Float
        get() = prefs.getFloat(KEY_WALLPAPER_FILTER_OPACITY, 0.40f)
        set(value) {
            prefs.edit().putFloat(KEY_WALLPAPER_FILTER_OPACITY, value.coerceIn(0f, 1f)).apply()
        }

    var wallpaperFilterColor: Int
        get() = prefs.getInt(KEY_WALLPAPER_FILTER_COLOR, 0xFF000000.toInt())
        set(value) {
            prefs.edit().putInt(KEY_WALLPAPER_FILTER_COLOR, value).apply()
        }

    // Helper methods for Wallpaper & Colors
    fun setCustomWallpaper(path: String, accentColor: Int? = null) {
        val editor = prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, path)
        if (accentColor != null) {
            editor.putInt(KEY_ADAPTIVE_ACCENT_COLOR, accentColor)
        }
        editor.apply()
    }

    fun clearCustomWallpaper() {
        prefs.edit()
            .remove(KEY_CUSTOM_WALLPAPER_PATH)
            .remove(KEY_ADAPTIVE_ACCENT_COLOR)
            .apply()
    }

    fun setSolidColors(preset: SolidColorPreset, backgroundColor: Int, accentColor: Int) {
        prefs.edit()
            .putString(KEY_SOLID_COLOR_PRESET, preset.name)
            .putInt(KEY_SOLID_BACKGROUND_COLOR, backgroundColor)
            .putInt(KEY_SOLID_ACCENT_COLOR, accentColor)
            .apply()
    }

    // Haptic Feedback
    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
        }

    // Top Reachability Spacing Mode
    var topSpacingMode: TopSpacingMode
        get() {
            val name = prefs.getString(KEY_TOP_SPACING_MODE, TopSpacingMode.NORMAL.name)
            return try {
                TopSpacingMode.valueOf(name ?: TopSpacingMode.NORMAL.name)
            } catch (e: Exception) {
                TopSpacingMode.NORMAL
            }
        }
        set(value) {
            prefs.edit().putString(KEY_TOP_SPACING_MODE, value.name).apply()
        }

    // Favorites
    fun getFavoriteApps(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_APPS, emptySet()) ?: emptySet()
    }

    fun setFavoriteApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_APPS, apps).apply()
    }

    fun addFavoriteApp(packageName: String) {
        val current = getFavoriteApps().toMutableSet()
        current.add(packageName)
        setFavoriteApps(current)
    }

    fun removeFavoriteApp(packageName: String) {
        val current = getFavoriteApps().toMutableSet()
        current.remove(packageName)
        setFavoriteApps(current)
    }

    fun toggleFavoriteApp(packageName: String): Boolean {
        val current = getFavoriteApps().toMutableSet()
        val isFav = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setFavoriteApps(current)
        return isFav
    }

    // Hidden Apps
    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
    }

    fun setHiddenApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, apps).apply()
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        val current = getHiddenApps().toMutableSet()
        if (hidden) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        setHiddenApps(current)
    }

    // Top Header Widgets
    var topWidgetId: Int
        get() = prefs.getInt(KEY_TOP_WIDGET_ID, -1)
        set(value) {
            prefs.edit().putInt(KEY_TOP_WIDGET_ID, value).apply()
        }

    fun getTopWidgetIds(): List<Int> {
        val raw = prefs.getString(KEY_TOP_WIDGET_IDS, null)
        if (!raw.isNullOrBlank()) {
            val ids = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (ids.isNotEmpty()) return ids
        }
        val legacy = topWidgetId
        return if (legacy != -1) listOf(legacy) else emptyList()
    }

    fun setTopWidgetIds(ids: List<Int>) {
        if (ids.isEmpty()) {
            prefs.edit().remove(KEY_TOP_WIDGET_IDS).apply()
            topWidgetId = -1
        } else {
            prefs.edit().putString(KEY_TOP_WIDGET_IDS, ids.joinToString(",")).apply()
            topWidgetId = ids.first()
        }
    }

    fun addTopWidgetId(id: Int) {
        val current = getTopWidgetIds().toMutableList()
        if (!current.contains(id)) {
            current.add(id)
            setTopWidgetIds(current)
        }
    }

    fun removeTopWidgetId(id: Int) {
        val current = getTopWidgetIds().toMutableList()
        current.remove(id)
        setTopWidgetIds(current)
    }

    // Per-App Inline Widget Exposure
    fun isWidgetExposedInline(packageName: String, isFavorite: Boolean): Boolean {
        val nonExposed = prefs.getStringSet(KEY_INLINE_NON_EXPOSED_APPS, emptySet()) ?: emptySet()
        if (nonExposed.contains(packageName)) return false
        val exposed = prefs.getStringSet(KEY_INLINE_EXPOSED_APPS, emptySet()) ?: emptySet()
        if (exposed.contains(packageName)) return true
        return isFavorite
    }

    fun setWidgetExposedInline(packageName: String, exposed: Boolean) {
        val exposedSet = (prefs.getStringSet(KEY_INLINE_EXPOSED_APPS, emptySet()) ?: emptySet()).toMutableSet()
        val nonExposedSet = (prefs.getStringSet(KEY_INLINE_NON_EXPOSED_APPS, emptySet()) ?: emptySet()).toMutableSet()
        if (exposed) {
            exposedSet.add(packageName)
            nonExposedSet.remove(packageName)
        } else {
            nonExposedSet.add(packageName)
            exposedSet.remove(packageName)
        }
        prefs.edit()
            .putStringSet(KEY_INLINE_EXPOSED_APPS, exposedSet)
            .putStringSet(KEY_INLINE_NON_EXPOSED_APPS, nonExposedSet)
            .apply()
    }

    fun toggleWidgetExposedInline(packageName: String, isFavorite: Boolean): Boolean {
        val current = isWidgetExposedInline(packageName, isFavorite)
        val next = !current
        setWidgetExposedInline(packageName, next)
        return next
    }

    // Pop-up Widgets (stored as packageName=id1,id2,id3 entries)
    fun getPopupWidgets(): Map<String, List<Int>> {
        val raw = prefs.getStringSet(KEY_POPUP_WIDGETS, emptySet()) ?: emptySet()
        val result = mutableMapOf<String, List<Int>>()
        for (item in raw) {
            val parts = item.split("=", limit = 2)
            if (parts.size == 2) {
                val ids = parts[1].split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                if (ids.isNotEmpty()) {
                    result[parts[0]] = ids
                }
            }
        }
        return result
    }

    fun getPopupWidgetsForApp(packageName: String): List<Int> {
        return getPopupWidgets()[packageName] ?: emptyList()
    }

    fun getPopupWidgetForApp(packageName: String): Int? {
        return getPopupWidgetsForApp(packageName).firstOrNull()
    }

    fun addPopupWidgetForApp(packageName: String, widgetId: Int) {
        val map = getPopupWidgets().toMutableMap()
        val list = (map[packageName] ?: emptyList()).toMutableList()
        if (!list.contains(widgetId)) {
            list.add(widgetId)
        }
        map[packageName] = list
        val rawSet = map.map { "${it.key}=${it.value.joinToString(",")}" }.toSet()
        prefs.edit().putStringSet(KEY_POPUP_WIDGETS, rawSet).apply()
    }

    fun removePopupWidgetForApp(packageName: String, widgetId: Int) {
        val map = getPopupWidgets().toMutableMap()
        val list = (map[packageName] ?: emptyList()).toMutableList()
        list.remove(widgetId)
        if (list.isEmpty()) {
            map.remove(packageName)
        } else {
            map[packageName] = list
        }
        val rawSet = map.map { "${it.key}=${it.value.joinToString(",")}" }.toSet()
        prefs.edit().putStringSet(KEY_POPUP_WIDGETS, rawSet).apply()
    }

    fun clearPopupWidgetsForApp(packageName: String) {
        val map = getPopupWidgets().toMutableMap()
        map.remove(packageName)
        val rawSet = map.map { "${it.key}=${it.value.joinToString(",")}" }.toSet()
        prefs.edit().putStringSet(KEY_POPUP_WIDGETS, rawSet).apply()
    }

    fun setPopupWidgetForApp(packageName: String, widgetId: Int?) {
        if (widgetId == null || widgetId == -1) {
            clearPopupWidgetsForApp(packageName)
        } else {
            clearPopupWidgetsForApp(packageName)
            addPopupWidgetForApp(packageName, widgetId)
        }
    }

    // Search History Queries
    fun getSearchHistory(): List<String> {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearchHistory().filter { it.lowercase() != trimmed.lowercase() }.toMutableList()
        current.add(0, trimmed)
        val limited = current.take(15)
        prefs.edit().putString(KEY_SEARCH_HISTORY, limited.joinToString("\n")).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = getSearchHistory().filter { it != query }
        prefs.edit().putString(KEY_SEARCH_HISTORY, current.joinToString("\n")).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    // Search Launch Counts (for learning frequency boost)
    fun getSearchLaunchCounts(): Map<String, Int> {
        val raw = prefs.getStringSet(KEY_SEARCH_LAUNCH_COUNTS, emptySet()) ?: emptySet()
        val result = mutableMapOf<String, Int>()
        for (item in raw) {
            val parts = item.split("=", limit = 2)
            if (parts.size == 2) {
                parts[1].toIntOrNull()?.let { count ->
                    result[parts[0]] = count
                }
            }
        }
        return result
    }

    fun recordAppLaunchFromSearch(packageName: String) {
        val map = getSearchLaunchCounts().toMutableMap()
        val currentCount = map[packageName] ?: 0
        map[packageName] = currentCount + 1
        val rawSet = map.map { "${it.key}=${it.value}" }.toSet()
        prefs.edit().putStringSet(KEY_SEARCH_LAUNCH_COUNTS, rawSet).apply()
    }

    fun getLaunchCount(packageName: String): Int {
        return getSearchLaunchCounts()[packageName] ?: 0
    }

    // Custom Radial Pins per Letter (Stored as "letter=pkg1,pkg2,pkg3" entries)
    fun getCustomRadialPins(): Map<String, List<String>> {
        val raw = prefs.getStringSet(KEY_CUSTOM_RADIAL_PINS, emptySet()) ?: emptySet()
        val result = mutableMapOf<String, List<String>>()
        for (item in raw) {
            val parts = item.split("=", limit = 2)
            if (parts.size == 2) {
                val letter = parts[0]
                val pkgs = parts[1].split(",").filter { it.isNotBlank() }
                result[letter] = pkgs
            }
        }
        return result
    }

    fun getCustomRadialPinsForLetter(letter: String): List<String> {
        return getCustomRadialPins()[letter] ?: emptyList()
    }

    fun setCustomRadialPinsForLetter(letter: String, packageNames: List<String>) {
        val map = getCustomRadialPins().toMutableMap()
        if (packageNames.isEmpty()) {
            map.remove(letter)
        } else {
            map[letter] = packageNames
        }
        val rawSet = map.map { "${it.key}=${it.value.joinToString(",")}" }.toSet()
        prefs.edit().putStringSet(KEY_CUSTOM_RADIAL_PINS, rawSet).apply()
    }

    fun clearCustomRadialPinsForLetter(letter: String) {
        setCustomRadialPinsForLetter(letter, emptyList())
    }

    // Widget Custom Height
    fun getWidgetCustomHeight(widgetId: Int): Int? {
        val key = "${KEY_WIDGET_CUSTOM_HEIGHT_PREFIX}$widgetId"
        return if (prefs.contains(key)) prefs.getInt(key, -1).takeIf { it > 0 } else null
    }

    fun setWidgetCustomHeight(widgetId: Int, heightDp: Int?) {
        val key = "${KEY_WIDGET_CUSTOM_HEIGHT_PREFIX}$widgetId"
        if (heightDp != null && heightDp > 0) {
            prefs.edit().putInt(key, heightDp).apply()
        } else {
            prefs.edit().remove(key).apply()
        }
    }

    // Widget Custom Column Span (1..8)
    fun getWidgetCustomSpan(widgetId: Int): Int? {
        val key = "${KEY_WIDGET_CUSTOM_SPAN_PREFIX}$widgetId"
        return if (prefs.contains(key)) prefs.getInt(key, -1).takeIf { it in 1..8 } else null
    }

    fun setWidgetCustomSpan(widgetId: Int, span: Int?) {
        val key = "${KEY_WIDGET_CUSTOM_SPAN_PREFIX}$widgetId"
        if (span != null && span in 1..8) {
            prefs.edit().putInt(key, span).apply()
        } else {
            prefs.edit().remove(key).apply()
        }
    }

    // Freeform 8-Column Grid Placement: row (0..), startCol (0..7), span (1..8) where startCol + span <= 8
    fun getWidgetGridPlacement(widgetId: Int): WidgetGridPlacement? {
        val key = "${KEY_WIDGET_GRID_PREFIX}$widgetId"
        val raw = prefs.getString(key, null)
        if (!raw.isNullOrBlank()) {
            val parts = raw.split("_")
            if (parts.size == 3) {
                val row = parts[0].toIntOrNull()
                val startCol = parts[1].toIntOrNull()
                val span = parts[2].toIntOrNull()
                if (row != null && startCol != null && span != null) {
                    val validRow = row.coerceAtLeast(0)
                    val validStart = startCol.coerceIn(0, 7)
                    val validSpan = span.coerceIn(1, 8 - validStart)
                    return WidgetGridPlacement(row = validRow, startCol = validStart, span = validSpan)
                }
            } else if (parts.size == 2) {
                val startCol = parts[0].toIntOrNull()
                val span = parts[1].toIntOrNull()
                if (startCol != null && span != null) {
                    val validStart = startCol.coerceIn(0, 7)
                    val validSpan = span.coerceIn(1, 8 - validStart)
                    return WidgetGridPlacement(row = 0, startCol = validStart, span = validSpan)
                }
            }
        }
        // Compatibility fallback from legacy span if present
        val legacySpan = getWidgetCustomSpan(widgetId)
        if (legacySpan != null) {
            val resolvedSpan = if (legacySpan in 1..4) (legacySpan * 2).coerceIn(1, 8) else legacySpan.coerceIn(1, 8)
            return WidgetGridPlacement(row = 0, startCol = 0, span = resolvedSpan)
        }
        return null
    }

    fun setWidgetGridPlacement(widgetId: Int, row: Int, startCol: Int, span: Int) {
        val key = "${KEY_WIDGET_GRID_PREFIX}$widgetId"
        val validRow = row.coerceAtLeast(0)
        val validStart = startCol.coerceIn(0, 7)
        val validSpan = span.coerceIn(1, 8 - validStart)
        prefs.edit().putString(key, "${validRow}_${validStart}_${validSpan}").apply()
    }

    fun removeWidgetGridPlacement(widgetId: Int) {
        val key = "${KEY_WIDGET_GRID_PREFIX}$widgetId"
        prefs.edit().remove(key).apply()
    }
}
