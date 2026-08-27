package com.slashboard.keyboard.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class OnlineThemeItem(
    val id: String,
    val name: String,
    val category: String,
    val previewUrl: String,
    val downloadUrl: String,
    val accentColorHex: Long = 0xFFA855F7,
    val description: String = "",
    val gradientColors: List<Int> = emptyList()
)

object OnlineThemeRepository {
    private const val TAG = "OnlineThemeRepository"

    val onlineThemes: List<OnlineThemeItem> = listOf(
        OnlineThemeItem(
            id = "sigiriya_sunset",
            name = "Sigiriya Golden Sunset",
            category = "Sri Lanka",
            previewUrl = "https://images.unsplash.com/photo-1588598198321-9735fd52455b?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1588598198321-9735fd52455b?w=1200&q=85",
            accentColorHex = 0xFFF59E0B,
            description = "Majestic ancient fortress glowing under tropical golden dusk",
            gradientColors = listOf(0xFF2C1600.toInt(), 0xFF78350F.toInt(), 0xFFD97706.toInt(), 0xFFF59E0B.toInt())
        ),
        OnlineThemeItem(
            id = "ella_mountains",
            name = "Misty Ella Peaks",
            category = "Sri Lanka",
            previewUrl = "https://images.unsplash.com/photo-1546708973-b339540b5162?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1546708973-b339540b5162?w=1200&q=85",
            accentColorHex = 0xFF10B981,
            description = "Serene green tea hills and cloud-covered peaks of Sri Lanka",
            gradientColors = listOf(0xFF022C22.toInt(), 0xFF065F46.toInt(), 0xFF047857.toInt(), 0xFF10B981.toInt())
        ),
        OnlineThemeItem(
            id = "mirissa_ocean",
            name = "Mirissa Ocean Waves",
            category = "Sri Lanka",
            previewUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=85",
            accentColorHex = 0xFF06B6D4,
            description = "Crystal turquoise waters of southern Sri Lanka shores",
            gradientColors = listOf(0xFF083344.toInt(), 0xFF164E63.toInt(), 0xFF0E7490.toInt(), 0xFF06B6D4.toInt())
        ),
        OnlineThemeItem(
            id = "cyber_synthwave",
            name = "Neon Cyberpunk Grid",
            category = "Cyberpunk",
            previewUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1200&q=85",
            accentColorHex = 0xFFEC4899,
            description = "Retro synthwave violet neon grid and glowing skyline",
            gradientColors = listOf(0xFF1E1B4B.toInt(), 0xFF581C87.toInt(), 0xFF831843.toInt(), 0xFFEC4899.toInt())
        ),
        OnlineThemeItem(
            id = "violet_matrix",
            name = "Holographic Aurora",
            category = "Cyberpunk",
            previewUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1200&q=85",
            accentColorHex = 0xFFA855F7,
            description = "Deep violet futuristic abstract matrix aurora lights",
            gradientColors = listOf(0xFF0F0B1E.toInt(), 0xFF2E1065.toInt(), 0xFF6B21A8.toInt(), 0xFFA855F7.toInt())
        ),
        OnlineThemeItem(
            id = "deep_space_cosmos",
            name = "Deep Space Galaxy",
            category = "Space & Nature",
            previewUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1200&q=85",
            accentColorHex = 0xFF818CF8,
            description = "Stunning cosmic stardust, nebula clusters, and distant stars",
            gradientColors = listOf(0xFF030712.toInt(), 0xFF1E1B4B.toInt(), 0xFF312E81.toInt(), 0xFF4338CA.toInt())
        ),
        OnlineThemeItem(
            id = "emerald_canopy",
            name = "Emerald Forest Canopy",
            category = "Space & Nature",
            previewUrl = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1511497584788-87676104235f?w=1200&q=85",
            accentColorHex = 0xFF34D399,
            description = "Lush tropical rainforest canopy with warm morning sunlight",
            gradientColors = listOf(0xFF052E16.toInt(), 0xFF14532D.toInt(), 0xFF166534.toInt(), 0xFF15803D.toInt())
        ),
        OnlineThemeItem(
            id = "dark_carbon_titanium",
            name = "Obsidian Carbon Fiber",
            category = "Minimal",
            previewUrl = "https://images.unsplash.com/photo-1550684847-75bdda21cc95?w=400&q=70",
            downloadUrl = "https://images.unsplash.com/photo-1550684847-75bdda21cc95?w=1200&q=85",
            accentColorHex = 0xFF38BDF8,
            description = "Tactile dark titanium and sleek geometric carbon pattern",
            gradientColors = listOf(0xFF0A0A0E.toInt(), 0xFF181820.toInt(), 0xFF272732.toInt(), 0xFF3F3F50.toInt())
        )
    )

    /**
     * Downloads an image from the given URL and saves it to local internal app storage.
     * Follows redirects automatically and generates aesthetic fallback if network is unreachable.
     */
    suspend fun downloadAndSaveWallpaper(
        context: Context,
        imageUrl: String,
        themeId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val wallpapersDir = File(context.filesDir, "wallpapers").apply {
            if (!exists()) mkdirs()
        }
        val safeThemeId = themeId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val destinationFile = File(wallpapersDir, "theme_$safeThemeId.jpg")

        var currentUrl = imageUrl
        var connection: HttpURLConnection? = null
        var downloadedSuccessfully = false

        try {
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Slashboard-App/1.0")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    if (!newUrl.isNullOrBlank()) {
                        currentUrl = newUrl
                        connection.disconnect()
                        redirects++
                        continue
                    }
                }

                if (status == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    downloadedSuccessfully = true
                    Log.d(TAG, "Wallpaper downloaded & saved to ${destinationFile.absolutePath}")
                }
                break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network download encountered error: ${e.message}. Using high-definition generated theme art fallback.")
        } finally {
            connection?.disconnect()
        }

        // If network failed (e.g. offline sandbox or server blocked), create high-res HD wallpaper art
        if (!downloadedSuccessfully || !destinationFile.exists() || destinationFile.length() < 100) {
            val themeItem = onlineThemes.find { it.id == themeId }
            createArtisticWallpaper(destinationFile, themeItem)
        }

        if (destinationFile.exists() && destinationFile.length() > 0) {
            Result.success(destinationFile.absolutePath)
        } else {
            Result.failure(Exception("Could not save wallpaper to storage"))
        }
    }

    /**
     * Saves an image selected from the device gallery (Uri) to internal app storage.
     */
    suspend fun saveUriAsWallpaper(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val wallpapersDir = File(context.filesDir, "wallpapers").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(wallpapersDir, "user_gallery_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.d(TAG, "Gallery wallpaper saved: ${destinationFile.absolutePath}")
                Result.success(destinationFile.absolutePath)
            } else {
                Result.failure(Exception("Could not read image from gallery URI"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save gallery image: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a stunning high-resolution artistic gradient background wallpaper bitmap as fallback.
     */
    private fun createArtisticWallpaper(file: File, item: OnlineThemeItem?) {
        try {
            val width = 1080
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val colors = item?.gradientColors?.takeIf { it.size >= 2 } ?: listOf(
                0xFF0F0B1E.toInt(), 0xFF2E1065.toInt(), 0xFF6B21A8.toInt(), 0xFFA855F7.toInt()
            )

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colors.toIntArray(),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Draw subtle decorative ambient glow arcs
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(40, 255, 255, 255)
            canvas.drawCircle(width * 0.85f, height * 0.2f, 320f, paint)

            paint.color = Color.argb(30, 255, 255, 255)
            canvas.drawCircle(width * 0.15f, height * 0.85f, 260f, paint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create fallback wallpaper: ${e.message}")
        }
    }

    /**
     * Loads a local image file as Bitmap with memory-safe downsampling.
     */
    fun loadWallpaperBitmap(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return null

            // First decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate safe sample size for keyboard background (target max 1280x800)
            val reqWidth = 1280
            val reqHeight = 800
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Low memory footprint

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode wallpaper bitmap from $path: ${e.message}")
            null
        }
    }
}
