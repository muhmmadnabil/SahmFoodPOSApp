package com.sahm.pos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform