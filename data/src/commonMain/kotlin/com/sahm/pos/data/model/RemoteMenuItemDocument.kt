package com.sahm.pos.data.model

data class RemoteMenuItemDocument(
    val id: String,
    val category: String?,
    val name: String?,
    val description: String?,
    val imageUrl: String?,
    val price: Long?,
)