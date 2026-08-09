package com.velocity.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) {
    private val iconCache = LruCache<String, Drawable>(50)
    private var cachedApps: List<AppModel> = emptyList()

    val currentApps: List<AppModel>
        get() = cachedApps

    suspend fun loadApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return@withContext emptyList()
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        val myUserHandle = Process.myUserHandle()

        val profiles: List<UserHandle> = try {
            launcherApps.profiles
        } catch (e: Exception) {
            listOf(myUserHandle)
        }

        val workAppsSet = preferencesManager.getWorkApps()
        val personalAppsSet = preferencesManager.getPersonalApps()
        val favoriteAppsSet = preferencesManager.getFavoriteApps()
        val hiddenAppsSet = preferencesManager.getHiddenApps()

        val appList = mutableListOf<AppModel>()

        for (handle in profiles) {
            val isWork = isWorkProfile(handle, myUserHandle, userManager)
            val activities: List<LauncherActivityInfo> = try {
                launcherApps.getActivityList(null, handle)
            } catch (e: Exception) {
                emptyList()
            }

            for (info in activities) {
                val label = info.label.toString()
                val packageName = info.applicationInfo.packageName
                val className = info.name

                val isWorkProfile = isWork
                val assignedFocus = when {
                    isWorkProfile || workAppsSet.contains(packageName) -> FocusMode.WORK
                    personalAppsSet.contains(packageName) -> FocusMode.PERSONAL
                    else -> FocusMode.ALL
                }
                val isFavorite = favoriteAppsSet.contains(packageName)
                val isHidden = hiddenAppsSet.contains(packageName)

                val cacheKey = "$packageName#${handle.hashCode()}"
                var cachedIcon = iconCache.get(cacheKey)
                if (cachedIcon == null) {
                    try {
                        cachedIcon = info.getIcon(0)
                        if (cachedIcon != null) {
                            iconCache.put(cacheKey, cachedIcon)
                        }
                    } catch (e: Exception) {
                        // Icon load failed; can be retried on getIcon()
                    }
                }

                val app = AppModel(
                    label = label,
                    packageName = packageName,
                    className = className,
                    userHandle = handle,
                    iconDrawable = cachedIcon,
                    isWorkProfile = isWorkProfile,
                    assignedFocus = assignedFocus,
                    isFavorite = isFavorite,
                    isHidden = isHidden
                )
                appList.add(app)
            }
        }

        val sorted = appList.sortedBy { it.label.lowercase() }
        cachedApps = sorted
        sorted
    }

    private fun isWorkProfile(
        handle: UserHandle,
        myUserHandle: UserHandle,
        userManager: UserManager?
    ): Boolean {
        if (handle == myUserHandle) return false
        if (userManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                if (userManager.isManagedProfile) return true
            } catch (e: Exception) {
                // Fallback check
            }
        }
        return handle != myUserHandle
    }

    fun getIcon(app: AppModel): Drawable? {
        val cacheKey = "${app.packageName}#${app.userHandle.hashCode()}"
        val cached = iconCache.get(cacheKey)
        if (cached != null) return cached

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return null
        val infoList = try {
            launcherApps.getActivityList(app.packageName, app.userHandle)
        } catch (e: Exception) {
            null
        }
        val info = infoList?.firstOrNull { it.name == app.className } ?: infoList?.firstOrNull()
        val icon = try {
            info?.getIcon(0)
        } catch (e: Exception) {
            null
        }
        if (icon != null) {
            iconCache.put(cacheKey, icon)
        }
        return icon
    }


    private fun matchesFocusMode(app: AppModel, mode: FocusMode): Boolean {
        return when (mode) {
            FocusMode.ALL -> true
            FocusMode.WORK -> app.isWorkProfile || app.assignedFocus == FocusMode.WORK
            FocusMode.PERSONAL -> !app.isWorkProfile && app.assignedFocus != FocusMode.WORK
        }
    }

    fun matchesQuery(app: AppModel, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()

        val labelLower = app.label.lowercase()
        if (labelLower.contains(q)) return true

        val pkgLower = app.packageName.lowercase()
        if (pkgLower.contains(q)) return true

        val initialisms = getInitialisms(app.label)
        if (initialisms.any { it.startsWith(q) || it == q }) return true

        return isFuzzySubsequence(labelLower, q)
    }

    private fun getInitialisms(label: String): List<String> {
        val result = mutableListOf<String>()

        // Word initials (e.g. "Play Store" -> "ps", "Google Maps" -> "gm")
        val words = label.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }
        if (words.size > 1) {
            val wordInitials = words.map { it.first().lowercaseChar() }.joinToString("")
            result.add(wordInitials)
        }

        // CamelCase / word boundary initials (e.g. "YouTube" -> "yt")
        val camelInitials = StringBuilder()
        var prevIsLower = false
        for (char in label) {
            if (char.isLetterOrDigit()) {
                if (camelInitials.isEmpty() || (char.isUpperCase() && prevIsLower)) {
                    camelInitials.append(char.lowercaseChar())
                }
                prevIsLower = char.isLowerCase()
            } else {
                prevIsLower = false
            }
        }
        if (camelInitials.isNotEmpty()) {
            result.add(camelInitials.toString())
        }

        return result
    }

    private fun isFuzzySubsequence(text: String, query: String): Boolean {
        var textIndex = 0
        var queryIndex = 0
        while (textIndex < text.length && queryIndex < query.length) {
            if (text[textIndex] == query[queryIndex]) {
                queryIndex++
            }
            textIndex++
        }
        return queryIndex == query.length
    }

    fun launchApp(app: AppModel) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val componentName = ComponentName(app.packageName, app.className)
        try {
            launcherApps?.startMainActivity(componentName, app.userHandle, null, null)
        } catch (e: Exception) {
            // Fallback launch
        }
    }

    fun getShortcuts(app: AppModel): List<ShortcutInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
        return try {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(app.packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            }
            launcherApps.getShortcuts(query, app.userHandle) ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getShortcutIcon(shortcut: ShortcutInfo): Drawable? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
        val density = context.resources.displayMetrics.densityDpi
        return try {
            launcherApps.getShortcutIconDrawable(shortcut, density)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun launchShortcut(shortcut: ShortcutInfo) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        try {
            launcherApps.startShortcut(shortcut, null, null)
        } catch (e: SecurityException) {
            // Graceful handling
        } catch (e: Exception) {
            // Graceful handling
        }
    }
}
