package com.velocity.launcher.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object WidgetSnapshotManager {
    private const val SNAPSHOT_DIR_NAME = "widget_snapshots"
    private const val SNAPSHOT_FILE_PREFIX = "widget_"
    private const val SNAPSHOT_FILE_EXT = ".png"
    private const val SNAPSHOT_TEMP_EXT = ".tmp"
    private const val PNG_COMPRESS_QUALITY = 100

    private fun getSnapshotDir(context: Context): File {
        val dir = File(context.cacheDir, SNAPSHOT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getSnapshotFile(context: Context, widgetId: Int): File {
        return File(File(context.cacheDir, SNAPSHOT_DIR_NAME), "$SNAPSHOT_FILE_PREFIX$widgetId$SNAPSHOT_FILE_EXT")
    }

    fun getSnapshot(context: Context, widgetId: Int): Bitmap? {
        return try {
            val file = getSnapshotFile(context, widgetId)
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun saveSnapshotAsync(context: Context, widgetId: Int, view: View) {
        if (view.width <= 0 || view.height <= 0) return

        val bitmap = try {
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        } catch (e: Throwable) {
            return
        }

        try {
            view.draw(Canvas(bitmap))
        } catch (e: Throwable) {
            bitmap.recycle()
            return
        }

        val appContext = context.applicationContext ?: context
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshotDir = getSnapshotDir(appContext)
                val targetFile = File(snapshotDir, "$SNAPSHOT_FILE_PREFIX$widgetId$SNAPSHOT_FILE_EXT")
                val tempFile = File(snapshotDir, "$SNAPSHOT_FILE_PREFIX$widgetId$SNAPSHOT_FILE_EXT$SNAPSHOT_TEMP_EXT")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESS_QUALITY, out)
                    out.flush()
                }
                if (tempFile.exists()) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    tempFile.renameTo(targetFile)
                }
            } catch (e: Throwable) {
                // Ignore snapshot saving errors
            } finally {
                bitmap.recycle()
            }
        }
    }

    fun deleteSnapshot(context: Context, widgetId: Int) {
        try {
            val file = getSnapshotFile(context, widgetId)
            if (file.exists()) {
                file.delete()
            }
            val tempFile = File(File(context.cacheDir, SNAPSHOT_DIR_NAME), "$SNAPSHOT_FILE_PREFIX$widgetId$SNAPSHOT_FILE_EXT$SNAPSHOT_TEMP_EXT")
            if (tempFile.exists()) {
                tempFile.delete()
            }
        } catch (e: Throwable) {
            // Ignore snapshot deletion errors
        }
    }
}
