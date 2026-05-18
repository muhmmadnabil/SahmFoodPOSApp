package com.sahm.pos.data.model

data class RemoteUserDocument(
    val id: String,
    val username: String?,
    val phone: String?,
    val password: String?,
    val createdAt: String,
    val isActive: Boolean?,
    val lastLoginAt: String,
)