package com.velocity.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class ExtractedColors(
    val dominantColor: Int? = null,
    val vibrantColor: Int? = null,
    val mutedColor: Int? = null,
    val lightVibrantColor: Int? = null,
    val darkVibrantColor: Int? = null,
    val lightMutedColor: Int? = null,
    val darkMutedColor: Int? = null,
    val swatchColors: List<Int> = emptyList()
) {
    val primaryAccent: Int
        get() = vibrantColor
            ?: dominantColor
            ?: lightVibrantColor
            ?: darkVibrantColor
            ?: mutedColor
            ?: swatchColors.firstOrNull()
            ?: 0xFF818CF8.toInt()
}

object WallpaperColorExtractor {
    const val WALLPAPER_FILE_NAME = "custom_wallpaper.jpg"
    private const val MAX_WALLPAPER_DIMENSION = 1920
    private const val MAX_PALETTE_SAMPLE_DIMENSION = 200

    private val DEFAULT_FALLBACK_COLORS = listOf(
        0xFF818CF8.toInt(), // Indigo
        0xFF6366F1.toInt(), // Primary Indigo
        0xFF38BDF8.toInt(), // Sky Blue
        0xFF34D399.toInt(), // Emerald
        0xFFFBBF24.toInt(), // Amber
        0xFFF87171.toInt(), // Rose
        0xFFA78BFA.toInt(), // Violet
        0xFFF472B6.toInt()  // Pink
    )

    fun getCustomWallpaperFile(context: Context): File {
        return File(context.filesDir, WALLPAPER_FILE_NAME)
    }

    fun hasCustomWallpaper(context: Context): Boolean {
        val file = getCustomWallpaperFile(context)
        return file.exists() && file.length() > 0
    }

    fun deleteCustomWallpaper(context: Context): Boolean {
        val file = getCustomWallpaperFile(context)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Opens system wallpaper chooser or display settings.
     */
    fun openSystemWallpaperChooser(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Pilih Wallpaper Sistem").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Safely copies an image from a content URI to internal app storage, downscaling to prevent OOM.
     */
    suspend fun copyUriToInternalStorage(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            // First decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            // Calculate sample size for downscaling
            var sampleSize = 1
            val maxDimension = max(srcWidth, srcHeight)
            while (maxDimension / sampleSize > MAX_WALLPAPER_DIMENSION) {
                sampleSize *= 2
            }

            // Decode the downsampled bitmap
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (decodedBitmap == null) return@withContext null

            // Scale precisely if still larger than target
            val currentMax = max(decodedBitmap.width, decodedBitmap.height)
            val finalBitmap = if (currentMax > MAX_WALLPAPER_DIMENSION) {
                val scale = MAX_WALLPAPER_DIMENSION.toFloat() / currentMax
                val targetW = (decodedBitmap.width * scale).roundToInt()
                val targetH = (decodedBitmap.height * scale).roundToInt()
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            // Save to internal storage
            val targetFile = getCustomWallpaperFile(context)
            val tempFile = File(context.filesDir, "${WALLPAPER_FILE_NAME}.tmp")
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            finalBitmap.recycle()

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }

            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a wallpaper bitmap safely from file.
     */
    fun loadWallpaperBitmap(file: File, maxDimension: Int = MAX_WALLPAPER_DIMENSION): Bitmap? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val srcMax = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            if (srcMax > maxDimension) {
                while (srcMax / sampleSize > maxDimension) {
                    sampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extracts palette colors from a file.
     */
    suspend fun extractPaletteFromFile(file: File): ExtractedColors = withContext(Dispatchers.Default) {
        if (!file.exists()) return@withContext ExtractedColors(swatchColors = DEFAULT_FALLBACK_COLORS)
        // Load small sample bitmap to speed up palette generation and prevent memory pressure
        val sampleBitmap = loadWallpaperBitmap(file, maxDimension = MAX_PALETTE_SAMPLE_DIMENSION)
            ?: return@withContext ExtractedColors(swatchColors = DEFAULT_FALLBACK_COLORS)
        try {
            extractPalette(sampleBitmap)
        } finally {
            sampleBitmap.recycle()
        }
    }

    /**
     * Extracts palette colors from a Bitmap using AndroidX Palette with Bitmap pixel sampling fallback.
     */
    fun extractPalette(bitmap: Bitmap): ExtractedColors {
        return try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            val dominant = palette.dominantSwatch?.rgb
            val vibrant = palette.vibrantSwatch?.rgb
            val muted = palette.mutedSwatch?.rgb
            val lightVibrant = palette.lightVibrantSwatch?.rgb
            val darkVibrant = palette.darkVibrantSwatch?.rgb
            val lightMuted = palette.lightMutedSwatch?.rgb
            val darkMuted = palette.darkMutedSwatch?.rgb

            val swatchesList = mutableListOf<Int>()

            // Add prominent semantic swatches first in priority order
            vibrant?.let { swatchesList.add(it) }
            lightVibrant?.let { swatchesList.add(it) }
            darkVibrant?.let { swatchesList.add(it) }
            dominant?.let { swatchesList.add(it) }
            muted?.let { swatchesList.add(it) }
            lightMuted?.let { swatchesList.add(it) }
            darkMuted?.let { swatchesList.add(it) }

            // Add other swatches sorted by population
            val sortedSwatches = palette.swatches.sortedByDescending { it.population }
            for (swatch in sortedSwatches) {
                swatchesList.add(swatch.rgb)
            }

            // Deduplicate colors that are identical or very close
            val distinctColors = filterDistinctColors(swatchesList)

            // If we have fewer than 6 colors, use pixel sampling fallback to complement
            val finalSwatches = if (distinctColors.size < 6) {
                val sampled = sampleBitmapPixels(bitmap)
                filterDistinctColors(distinctColors + sampled + DEFAULT_FALLBACK_COLORS).take(10)
            } else {
                distinctColors.take(10)
            }

            ExtractedColors(
                dominantColor = dominant ?: finalSwatches.firstOrNull(),
                vibrantColor = vibrant ?: finalSwatches.firstOrNull(),
                mutedColor = muted,
                lightVibrantColor = lightVibrant,
                darkVibrantColor = darkVibrant,
                lightMutedColor = lightMuted,
                darkMutedColor = darkMuted,
                swatchColors = finalSwatches
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to pixel sampling if Palette fails
            val fallbackSwatches = sampleBitmapPixels(bitmap)
            val colors = if (fallbackSwatches.isNotEmpty()) fallbackSwatches else DEFAULT_FALLBACK_COLORS
            ExtractedColors(
                dominantColor = colors.firstOrNull(),
                vibrantColor = colors.firstOrNull(),
                swatchColors = colors.take(8)
            )
        }
    }

    /**
     * Fallback pixel sampling method: samples a grid of pixels from the bitmap to extract dominant colors.
     */
    private fun sampleBitmapPixels(bitmap: Bitmap): List<Int> {
        val colors = mutableListOf<Int>()
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return DEFAULT_FALLBACK_COLORS

        val stepX = max(1, width / 12)
        val stepY = max(1, height / 12)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                // Filter out fully transparent or near-black/near-white monotone
                val alpha = (pixel shr 24) and 0xFF
                if (alpha > 50) {
                    colors.add(pixel)
                }
            }
        }

        // Count frequency of quantized colors (reduce to 4-bit per channel to cluster similar colors)
        val quantizedMap = mutableMapOf<Int, MutableList<Int>>()
        for (color in colors) {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val qr = (r / 32) * 32
            val qg = (g / 32) * 32
            val qb = (b / 32) * 32
            val key = (0xFF shl 24) or (qr shl 16) or (qg shl 8) or qb
            quantizedMap.getOrPut(key) { mutableListOf() }.add(color)
        }

        val sortedClusters = quantizedMap.entries.sortedByDescending { it.value.size }
        val representativeColors = sortedClusters.map { cluster ->
            // Use average or first color of cluster
            cluster.value.first()
        }

        return filterDistinctColors(representativeColors)
    }

    /**
     * Filters out colors that are visually too close to already chosen colors.
     */
    private fun filterDistinctColors(colors: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        for (color in colors) {
            val opaqueColor = color or (0xFF shl 24)
            val isTooClose = result.any { existing ->
                colorDistance(existing, opaqueColor) < 36.0
            }
            if (!isTooClose) {
                result.add(opaqueColor)
            }
        }
        return result
    }

    /**
     * Euclidean distance in RGB color space.
     */
    private fun colorDistance(c1: Int, c2: Int): Double {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2

        return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }
}
