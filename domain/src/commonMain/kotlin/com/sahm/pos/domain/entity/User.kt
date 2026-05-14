package com.sahm.pos.domain.entity

data class User(
    val username: String,
    val createdAt: String,
    val isActive: Boolean,
    val lastLoginAt: String,
    val password: String,
    val id: String,
)
