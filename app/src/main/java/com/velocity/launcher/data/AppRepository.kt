package com.velocity.launcher.data

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.DisplayMetrics
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class AppRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) {
    private val iconCache = LruCache<String, Drawable>(150)
    private val bitmapCache = LruCache<String, ImageBitmap>(200)
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

        val favoriteAppsSet = preferencesManager.getFavoriteApps()
        val hiddenAppsSet = preferencesManager.getHiddenApps()
        val popupWidgetsMap = preferencesManager.getPopupWidgets()

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

                val isFavorite = favoriteAppsSet.contains(packageName)
                val isHidden = hiddenAppsSet.contains(packageName)
                val popupWidgetIds = popupWidgetsMap[packageName] ?: emptyList()

                val cacheKey = "$packageName#${handle.hashCode()}"
                var cachedIcon = iconCache.get(cacheKey)
                if (cachedIcon == null) {
                    try {
                        cachedIcon = info.getIcon(0)
                        if (cachedIcon != null) {
                            iconCache.put(cacheKey, cachedIcon)
                        }
                    } catch (e: Exception) {
                        // Icon load failed; retry on getIcon()
                    }
                }

                // Pre-warm ImageBitmap cache
                if (cachedIcon != null) {
                    val bitmapKey = "$packageName#${handle.hashCode()}#128"
                    if (bitmapCache.get(bitmapKey) == null) {
                        try {
                            bitmapCache.put(bitmapKey, cachedIcon.toBitmap(128, 128).asImageBitmap())
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }

                val normalizedLabel = label.lowercase()
                val wordPrefixes = computeWordPrefixes(label)
                val initialisms = computeInitialisms(label, wordPrefixes)

                val app = AppModel(
                    label = label,
                    packageName = packageName,
                    className = className,
                    userHandle = handle,
                    isWorkProfile = isWork,
                    isFavorite = isFavorite,
                    isHidden = isHidden,
                    popupWidgetIds = popupWidgetIds,
                    normalizedLabel = normalizedLabel,
                    wordPrefixes = wordPrefixes,
                    initialisms = initialisms
                )
                appList.add(app)
            }
        }

        val sorted = appList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        cachedApps = sorted
        sorted
    }

    private fun computeWordPrefixes(label: String): List<String> {
        return label.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotEmpty() }
    }

    private fun computeInitialisms(label: String, words: List<String>): List<String> {
        val result = mutableListOf<String>()

        if (words.size > 1) {
            val wordInitials = words.map { it.first() }.joinToString("")
            result.add(wordInitials)
        }

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
        val camelStr = camelInitials.toString()
        if (camelStr.isNotEmpty() && !result.contains(camelStr)) {
            result.add(camelStr)
        }

        return result
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
                // Fallback
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

    fun getIconBitmap(app: AppModel, sizePx: Int = 128): ImageBitmap? {
        val cacheKey = "${app.packageName}#${app.userHandle.hashCode()}#$sizePx"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null) return cached

        val drawable = getIcon(app) ?: return null
        val bitmap = try {
            drawable.toBitmap(sizePx, sizePx).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        if (bitmap != null) {
            bitmapCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    fun filterAndRankApps(
        apps: List<AppModel>,
        query: String,
        launchCounts: Map<String, Int>
    ): List<AppModel> {
        val nonHidden = apps.filter { !it.isHidden }
        if (query.isBlank()) {
            // When query is empty, sort by search launch frequency
            return nonHidden
                .sortedByDescending { launchCounts[it.packageName] ?: 0 }
        }

        val q = query.trim().lowercase()

        return nonHidden
            .mapNotNull { app ->
                val score = calculateMatchScore(app, q)
                if (score > 0) {
                    val boost = (launchCounts[app.packageName] ?: 0) * 40
                    app to (score + boost)
                } else {
                    null
                }
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun calculateMatchScore(app: AppModel, query: String): Int {
        val labelLower = app.normalizedLabel
        val pkgLower = app.packageName.lowercase()

        // Launcher settings search keywords support
        val myPkg = try { context?.packageName } catch (e: Exception) { null }
        if (myPkg != null && app.packageName == myPkg) {
            val launcherKeywords = listOf(
                "settings", "setting", "pengaturan",
                "preferences", "preference", "launcher",
                "nirantara"
            )
            for (keyword in launcherKeywords) {
                if (keyword == query) {
                    return 2000
                }
                if (keyword.startsWith(query)) {
                    return 1500 - (keyword.length - query.length)
                }
                if (keyword.contains(query)) {
                    return 600
                }
            }
        }

        // 1. Exact match on label
        if (labelLower == query) return 2000

        // 2. Exact prefix match on label
        if (labelLower.startsWith(query)) return 1500 - labelLower.length

        // 3. Word start matches (e.g. "Google Maps" matching "maps" or "google")
        val words = app.wordPrefixes
        for (index in words.indices) {
            if (words[index].startsWith(query)) {
                return 1000 - (index * 50)
            }
        }

        // 4. Initialisms matches (e.g. "ps" for "Play Store", "yt" for "YouTube")
        val initialisms = app.initialisms
        for (i in initialisms.indices) {
            val init = initialisms[i]
            if (init == query) return 800
            if (init.startsWith(query)) return 700
        }

        // 5. Substring match in label
        val subIndex = labelLower.indexOf(query)
        if (subIndex >= 0) {
            return 500 - subIndex
        }

        // 6. Subsequence match in label
        if (isFuzzySubsequence(labelLower, query)) {
            return 300
        }

        // 7. Match in package name
        if (pkgLower.contains(query)) {
            return 100
        }

        return 0
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

    fun getLaunchCount(packageName: String): Int {
        return preferencesManager.getLaunchCount(packageName)
    }

    fun launchApp(app: AppModel) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val componentName = ComponentName(app.packageName, app.className)
        try {
            launcherApps?.startMainActivity(componentName, app.userHandle, null, null)
        } catch (e: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                launchIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            } catch (ex: Exception) {
                // Ignore launch failures
            }
        }
    }

    fun hasShortcutHostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
        return try {
            launcherApps.hasShortcutHostPermission()
        } catch (e: Exception) {
            false
        }
    }

    fun getShortcuts(app: AppModel): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        if (!hasShortcutHostPermission()) return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
        return try {
            var flags = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                flags = flags or LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED
            }
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(app.packageName)
                setQueryFlags(flags)
            }
            val rawList = launcherApps.getShortcuts(query, app.userHandle) ?: emptyList()
            rawList.filter { it.isEnabled }.sortedBy { it.rank }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getShortcutIcon(shortcut: ShortcutInfo): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return null
        val density = context.resources.displayMetrics.densityDpi
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    launcherApps.getShortcutBadgedIconDrawable(shortcut, density)
                } catch (e: Exception) {
                    launcherApps.getShortcutIconDrawable(shortcut, density)
                }
            } else {
                launcherApps.getShortcutIconDrawable(shortcut, density)
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getShortcutIconBitmap(shortcut: ShortcutInfo, sizePx: Int = 96): ImageBitmap? {
        val userHash = shortcut.userHandle?.hashCode() ?: 0
        val cacheKey = "shortcut#${shortcut.`package`}#${shortcut.id}#${userHash}#$sizePx"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null) return cached

        val drawable = getShortcutIcon(shortcut) ?: return null
        val bitmap = try {
            drawable.toBitmap(sizePx, sizePx).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        if (bitmap != null) {
            bitmapCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    fun launchShortcut(shortcut: ShortcutInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        try {
            launcherApps.startShortcut(shortcut, null, null)
        } catch (e: SecurityException) {
            // Graceful handling
        } catch (e: Exception) {
            try {
                launcherApps.startShortcut(shortcut.`package`, shortcut.id, null, null, shortcut.userHandle)
            } catch (ex: Exception) {
                // Graceful handling
            }
        }
    }

    fun openAppInfo(app: AppModel) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun uninstallApp(app: AppModel) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun searchWeb(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, trimmed)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val url = "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                // Fallback
            }
        }
    }

    fun openClock() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory("android.intent.category.APP_CLOCK")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Fallback
            }
        }
    }

    fun openCalendar() {
        try {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = builder.build()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Fallback
            }
        }
    }
}

