package com.velocity.launcher.data

import android.content.Context
import android.content.SharedPreferences

enum class DisplayMode {
    GRID,
    TEXT_ONLY
}

enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED_BLACK
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "velocity_launcher_prefs"
        private const val KEY_ACTIVE_FOCUS_MODE = "active_focus_mode"
        private const val KEY_WORK_APPS = "work_apps"
        private const val KEY_PERSONAL_APPS = "personal_apps"
        private const val KEY_FAVORITE_APPS = "favorite_apps"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    var activeFocusMode: FocusMode
        get() {
            val name = prefs.getString(KEY_ACTIVE_FOCUS_MODE, FocusMode.ALL.name)
            return try {
                FocusMode.valueOf(name ?: FocusMode.ALL.name)
            } catch (e: Exception) {
                FocusMode.ALL
            }
        }
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_FOCUS_MODE, value.name).apply()
        }

    fun getWorkApps(): Set<String> {
        return prefs.getStringSet(KEY_WORK_APPS, emptySet()) ?: emptySet()
    }

    fun setWorkApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_WORK_APPS, apps).apply()
    }

    fun addWorkApp(packageName: String) {
        val current = getWorkApps().toMutableSet()
        current.add(packageName)
        setWorkApps(current)
    }

    fun removeWorkApp(packageName: String) {
        val current = getWorkApps().toMutableSet()
        current.remove(packageName)
        setWorkApps(current)
    }

    fun getPersonalApps(): Set<String> {
        return prefs.getStringSet(KEY_PERSONAL_APPS, emptySet()) ?: emptySet()
    }

    fun setPersonalApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_PERSONAL_APPS, apps).apply()
    }

    fun addPersonalApp(packageName: String) {
        val current = getPersonalApps().toMutableSet()
        current.add(packageName)
        setPersonalApps(current)
    }

    fun removePersonalApp(packageName: String) {
        val current = getPersonalApps().toMutableSet()
        current.remove(packageName)
        setPersonalApps(current)
    }

    fun getFavoriteApps(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_APPS, emptySet()) ?: emptySet()
    }

    fun setFavoriteApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_APPS, apps).apply()
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

    var displayMode: DisplayMode
        get() {
            val name = prefs.getString(KEY_DISPLAY_MODE, DisplayMode.GRID.name)
            return try {
                DisplayMode.valueOf(name ?: DisplayMode.GRID.name)
            } catch (e: Exception) {
                DisplayMode.GRID
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DISPLAY_MODE, value.name).apply()
        }

    var gridColumnCount: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 4)
        set(value) {
            val clamped = value.coerceIn(3, 5)
            prefs.edit().putInt(KEY_GRID_COLUMNS, clamped).apply()
        }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.DARK.name)
            } catch (e: Exception) {
                ThemeMode.DARK
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }
}
