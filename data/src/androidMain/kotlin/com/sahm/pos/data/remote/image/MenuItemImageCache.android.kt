package com.sahm.pos.data.remote.image

import com.sahm.pos.data.local.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

actual fun createMenuItemImageCache(platformContext: PlatformContext): MenuItemImageCache =
    AndroidMenuItemImageCache(platformContext)

private class AndroidMenuItemImageCache(
    private val platformContext: PlatformContext,
) : MenuItemImageCache {
    override suspend fun cacheImage(itemId: String, imageUrl: String): String? =
        runCatching {
            withContext(Dispatchers.IO) {
                val directory = File(platformContext.context.cacheDir, "menu_item_images")
                directory.mkdirs()
                val file = File(directory, "${itemId.sanitizeFileName()}${imageUrl.extension()}")
                URL(imageUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file.toURI().toString()
            }
        }.getOrNull()
}

private fun String.sanitizeFileName(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "_")

private fun String.extension(): String {
    val path = substringBefore('?')
    val extension = path.substringAfterLast('.', missingDelimiterValue = "")
    return if (extension.isBlank()) ".img" else ".$extension"
}
