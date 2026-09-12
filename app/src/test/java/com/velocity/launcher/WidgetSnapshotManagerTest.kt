package com.velocity.launcher

import android.content.Context
import android.content.ContextWrapper
import com.velocity.launcher.ui.widget.WidgetSnapshotManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WidgetSnapshotManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var cacheDir: File

    private class TestContext(private val cacheDirectory: File) : ContextWrapper(null) {
        override fun getCacheDir(): File = cacheDirectory
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        cacheDir = temporaryFolder.newFolder("cache")
        mockContext = TestContext(cacheDir)
    }

    @Test
    fun testGetSnapshot_nonExistentFile_returnsNull() {
        val result = WidgetSnapshotManager.getSnapshot(mockContext, 999)
        assertNull(result)
    }

    @Test
    fun testDeleteSnapshot_existingFile_deletesCleanly() {
        val widgetDir = File(cacheDir, "widget_snapshots")
        widgetDir.mkdirs()
        val snapshotFile = File(widgetDir, "widget_123.png")
        snapshotFile.writeText("fake-png-content")
        val tmpFile = File(widgetDir, "widget_123.png.tmp")
        tmpFile.writeText("fake-tmp-content")

        assertTrue(snapshotFile.exists())
        assertTrue(tmpFile.exists())

        WidgetSnapshotManager.deleteSnapshot(mockContext, 123)

        assertFalse(snapshotFile.exists())
        assertFalse(tmpFile.exists())
    }

    @Test
    fun testDeleteSnapshot_nonExistentFile_noError() {
        WidgetSnapshotManager.deleteSnapshot(mockContext, 888)
        val file = File(File(cacheDir, "widget_snapshots"), "widget_888.png")
        assertFalse(file.exists())
    }
}
