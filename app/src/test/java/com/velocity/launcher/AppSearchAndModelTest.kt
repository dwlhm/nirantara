package com.velocity.launcher

import android.os.Process
import android.os.UserHandle
import com.velocity.launcher.data.AppModel
import com.velocity.launcher.data.AppRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchAndModelTest {

    private fun createAppModel(
        label: String,
        packageName: String,
        className: String = "MainActivity",
        isFavorite: Boolean = false,
        isHidden: Boolean = false
    ): AppModel {
        val words = label.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }
        val initialisms = mutableListOf<String>()
        if (words.size > 1) {
            initialisms.add(words.map { it.first() }.joinToString(""))
        }
        val camel = StringBuilder()
        var prevLower = false
        for (c in label) {
            if (c.isLetterOrDigit()) {
                if (camel.isEmpty() || (c.isUpperCase() && prevLower)) {
                    camel.append(c.lowercaseChar())
                }
                prevLower = c.isLowerCase()
            } else {
                prevLower = false
            }
        }
        if (camel.isNotEmpty() && !initialisms.contains(camel.toString())) {
            initialisms.add(camel.toString())
        }

        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        val userHandle = allocateMethod.invoke(unsafe, UserHandle::class.java) as UserHandle

        return AppModel(
            label = label,
            packageName = packageName,
            className = className,
            userHandle = userHandle,
            isFavorite = isFavorite,
            isHidden = isHidden,
            normalizedLabel = label.lowercase(),
            wordPrefixes = words,
            initialisms = initialisms
        )
    }

    @Test
    fun testAppModel_immutableAndPrecomputedMetadata() {
        val app = createAppModel("Google Maps", "com.google.android.apps.maps")
        assertEquals("google maps", app.normalizedLabel)
        assertEquals(listOf("google", "maps"), app.wordPrefixes)
        assertTrue(app.initialisms.contains("gm"))
        assertEquals("com.google.android.apps.maps/MainActivity#${app.userHandle.hashCode()}", app.id)

        // Copy test
        val favApp = app.copy(isFavorite = true)
        assertTrue(favApp.isFavorite)
        assertFalse(app.isFavorite)
    }

    @Test
    fun testInitialisms_camelCase() {
        val yt = createAppModel("YouTube", "com.google.android.youtube")
        assertEquals(listOf("youtube"), yt.wordPrefixes)
        assertTrue(yt.initialisms.contains("yt"))

        val ps = createAppModel("PlayStore", "com.android.vending")
        assertTrue(ps.initialisms.contains("ps"))
    }

    @Test
    fun testFilterAndRankApps_searchRanking() {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        val repo = allocateMethod.invoke(unsafe, AppRepository::class.java) as AppRepository

        val apps = listOf(
            createAppModel("Google Maps", "com.google.android.apps.maps"),
            createAppModel("Gmail", "com.google.android.gm"),
            createAppModel("Google Photos", "com.google.android.apps.photos"),
            createAppModel("YouTube", "com.google.android.youtube"),
            createAppModel("Play Store", "com.android.vending"),
            createAppModel("Settings", "com.android.settings", isHidden = true)
        )

        // 1. Exact match
        val gmailResult = repo.filterAndRankApps(apps, "gmail", emptyMap())
        assertEquals("Gmail", gmailResult.first().label)

        // 2. Initialism match: "gm" matches "Google Maps"
        val gmResult = repo.filterAndRankApps(apps, "gm", emptyMap())
        assertTrue(gmResult.any { it.label == "Google Maps" })

        // 3. Word start match: "maps" matches "Google Maps"
        val mapsResult = repo.filterAndRankApps(apps, "maps", emptyMap())
        assertEquals("Google Maps", mapsResult.first().label)

        // 4. Initialism match: "ps" matches "Play Store"
        val psResult = repo.filterAndRankApps(apps, "ps", emptyMap())
        assertEquals("Play Store", psResult.first().label)

        // 5. Initialism match: "yt" matches "YouTube"
        val ytResult = repo.filterAndRankApps(apps, "yt", emptyMap())
        assertEquals("YouTube", ytResult.first().label)

        // 6. Hidden apps excluded
        val settingsResult = repo.filterAndRankApps(apps, "settings", emptyMap())
        assertTrue(settingsResult.isEmpty())

        // 7. Frequency boost elevates app
        val launchCounts = mapOf("com.google.android.apps.photos" to 10)
        val gResult = repo.filterAndRankApps(apps, "g", launchCounts)
        assertEquals("Google Photos", gResult.first().label)
    }

    @Test
    fun testLauncherKeywordsMatching() {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        val repo = allocateMethod.invoke(unsafe, AppRepository::class.java) as AppRepository

        val mockContext = object : android.content.ContextWrapper(null) {
            override fun getPackageName(): String = "com.velocity.launcher"
        }

        val contextField = AppRepository::class.java.getDeclaredField("context").apply { isAccessible = true }
        contextField.set(repo, mockContext)

        val launcherApp = createAppModel("Velocity", "com.velocity.launcher")
        val otherApp = createAppModel("Some App", "com.example.someapp")
        val apps = listOf(otherApp, launcherApp)

        val keywords = listOf("settings", "setting", "pengaturan", "preferences", "preference", "launcher", "nirantara")
        for (kw in keywords) {
            val result = repo.filterAndRankApps(apps, kw, emptyMap())
            assertEquals("Expected launcher app to match keyword $kw", "Velocity", result.firstOrNull()?.label)
        }

        val prefixResult = repo.filterAndRankApps(apps, "sett", emptyMap())
        assertEquals("Velocity", prefixResult.firstOrNull()?.label)
    }
}
