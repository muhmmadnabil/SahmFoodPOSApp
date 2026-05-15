package com.sahm.pos.data.remote.image

import com.sahm.pos.data.local.PlatformContext

interface MenuItemImageCache {
    suspend fun cacheImage(itemId: String, imageUrl: String): String?
}

expect fun createMenuItemImageCache(platformContext: PlatformContext): MenuItemImageCache
