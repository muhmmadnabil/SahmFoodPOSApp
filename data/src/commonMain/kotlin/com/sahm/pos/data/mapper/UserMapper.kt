package com.sahm.pos.data.mapper

import com.sahm.pos.data.model.RemoteUserDocument
import com.sahm.pos.domain.entity.User

internal fun RemoteUserDocument.toUserOrNull(lastSyncAt: Long): User? {
    if (id.isBlank()) return null
    val validUsername = username?.takeIf { it.isNotBlank() } ?: return null
    val validPhone = phone?.takeIf { it.isNotBlank() } ?: return null
    val validPassword = password?.takeIf { it.isNotBlank() } ?: return null

    return User(
        id = id,
        username = validUsername,
        phone = validPhone,
        password = validPassword,
        createdAt = createdAt,
        isActive = isActive ?: true,
        lastLoginAt = lastLoginAt,
        lastSyncAt = lastSyncAt,
    )
}
