package com.sahm.pos.domain.entity

data class User(
    val id: String,
    val username: String,
    val phone: String,
    val createdAt: String,
    val isActive: Boolean,
    val lastLoginAt: String,
    val password: String,
)
