package com.sahm.pos.domain.entity

data class MenuItem(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Long,
    val lastSyncedAt: Long? = null,
    val isActive: Boolean = true,
    val localImageUrl: String? = null,
)