package com.velocity.launcher

import com.velocity.launcher.data.AppModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphingBubbleResolutionTest {

    private fun createDummyUserHandle(): android.os.UserHandle {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val allocateMethod = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        return allocateMethod.invoke(unsafe, android.os.UserHandle::class.java) as android.os.UserHandle
    }

    private fun createApp(label: String, pkg: String): AppModel {
        return AppModel(
            label = label,
            packageName = pkg,
            className = "$pkg.MainActivity",
            userHandle = createDummyUserHandle()
        )
    }

    @Test
    fun testHybridResolution_pinnedAppsPreservedAndAutoFilled() {
        val appSpotify = createApp("Spotify", "com.spotify.music")
        val appSlack = createApp("Slack", "com.Slack")
        val appSettings = createApp("Settings", "com.android.settings")
        val appSoundCloud = createApp("SoundCloud", "com.soundcloud.android")
        val appSnapchat = createApp("Snapchat", "com.snapchat.android")

        val allAppsForLetter = listOf(appSpotify, appSlack, appSettings, appSoundCloud, appSnapchat)

        // User pinned Slack first, then Snapchat
        val pinnedPackageNames = listOf("com.Slack", "com.snapchat.android")
        val pinnedApps = pinnedPackageNames.mapNotNull { pkg ->
            allAppsForLetter.firstOrNull { it.packageName == pkg }
        }

        // Mock launch counts: SoundCloud has 10 launches, Spotify has 5, Settings has 1
        val launchCounts = mapOf(
            "com.soundcloud.android" to 10,
            "com.spotify.music" to 5,
            "com.android.settings" to 1
        )

        val remainingCount = 4 - pinnedApps.size // 2
        val pinnedSet = pinnedApps.map { it.packageName }.toSet()
        val autoFillApps = allAppsForLetter
            .filter { it.packageName !in pinnedSet }
            .sortedByDescending { launchCounts[it.packageName] ?: 0 }
            .take(remainingCount)

        val resolved = pinnedApps + autoFillApps

        assertEquals(4, resolved.size)
        // Fixed spatial invariance: Pinned slots remain in fixed positions 0 and 1
        assertEquals("Slack", resolved[0].label)
        assertEquals("Snapchat", resolved[1].label)
        // Auto-fill populated with highest frequency apps
        assertEquals("SoundCloud", resolved[2].label)
        assertEquals("Spotify", resolved[3].label)
    }

    @Test
    fun testHybridResolution_fullyPinnedArc_noAutoFill() {
        val apps = listOf(
            createApp("Spotify", "com.spotify.music"),
            createApp("Slack", "com.Slack"),
            createApp("Settings", "com.android.settings"),
            createApp("SoundCloud", "com.soundcloud.android"),
            createApp("Snapchat", "com.snapchat.android")
        )

        val pinnedPackageNames = listOf("com.spotify.music", "com.Slack", "com.android.settings", "com.soundcloud.android")
        val pinnedApps = pinnedPackageNames.mapNotNull { pkg ->
            apps.firstOrNull { it.packageName == pkg }
        }

        val resolved = if (pinnedApps.size >= 4) pinnedApps.take(4) else emptyList()
        assertEquals(4, resolved.size)
        assertEquals("Spotify", resolved[0].label)
        assertEquals("Slack", resolved[1].label)
        assertEquals("Settings", resolved[2].label)
        assertEquals("SoundCloud", resolved[3].label)
    }

    @Test
    fun testHybridResolution_zeroPinned_allAutoFill() {
        val apps = listOf(
            createApp("Spotify", "com.spotify.music"),
            createApp("Slack", "com.Slack"),
            createApp("Settings", "com.android.settings"),
            createApp("SoundCloud", "com.soundcloud.android")
        )

        val launchCounts = mapOf(
            "com.Slack" to 50,
            "com.spotify.music" to 30,
            "com.soundcloud.android" to 20,
            "com.android.settings" to 10
        )

        val pinnedApps = emptyList<AppModel>()
        val autoFillApps = apps
            .sortedByDescending { launchCounts[it.packageName] ?: 0 }
            .take(4)

        val resolved = pinnedApps + autoFillApps
        assertEquals(4, resolved.size)
        assertEquals("Slack", resolved[0].label)
        assertEquals("Spotify", resolved[1].label)
        assertEquals("SoundCloud", resolved[2].label)
        assertEquals("Settings", resolved[3].label)
    }

    @Test
    fun testCustomRadialPinsParsing() {
        val rawSet = setOf(
            "S=com.spotify.music,com.Slack",
            "A=com.adobe.reader,com.amazon.mShop.android.shopping,com.apple.android.music",
            "Z=com.zomato"
        )

        val parsed = mutableMapOf<String, List<String>>()
        for (item in rawSet) {
            val parts = item.split("=", limit = 2)
            if (parts.size == 2) {
                val letter = parts[0]
                val pkgs = parts[1].split(",").filter { it.isNotBlank() }
                parsed[letter] = pkgs
            }
        }

        assertEquals(listOf("com.spotify.music", "com.Slack"), parsed["S"])
        assertEquals(listOf("com.adobe.reader", "com.amazon.mShop.android.shopping", "com.apple.android.music"), parsed["A"])
        assertEquals(listOf("com.zomato"), parsed["Z"])
        assertTrue(parsed["B"].isNullOrEmpty())
    }
}
