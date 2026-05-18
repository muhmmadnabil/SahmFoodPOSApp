package com.sahm.pos.data.mapper

import com.sahm.pos.data.model.RemoteMenuItemDocument
import com.sahm.pos.domain.entity.MenuItem

internal fun RemoteMenuItemDocument.toMenuItemOrNull(
    lastSyncedAt: Long,
    localImageUrl: String?,
): MenuItem? {
    if (id.isBlank()) return null
    val validCategory = category?.takeIf { it.isNotBlank() } ?: return null
    val validName = name?.takeIf { it.isNotBlank() } ?: return null
    val validDescription = description?.takeIf { it.isNotBlank() } ?: return null
    val validImageUrl = imageUrl?.takeIf { it.isNotBlank() } ?: return null
    val validPrice = price?.takeIf { it >= 0 } ?: return null

    return MenuItem(
        id = id,
        category = validCategory,
        name = validName,
        description = validDescription,
        imageUrl = validImageUrl,
        localImageUrl = localImageUrl,
        price = validPrice,
        lastSyncedAt = lastSyncedAt,
        isActive = true,
    )
}
