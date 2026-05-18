package com.sahm.pos.data.remote.image

import com.sahm.pos.data.local.PlatformContext

actual fun createMenuItemImageCache(platformContext: PlatformContext): MenuItemImageCache =
    IosMenuItemImageCache

private object IosMenuItemImageCache : MenuItemImageCache {
    override suspend fun cacheImage(itemId: String, imageUrl: String): String? = null
}
