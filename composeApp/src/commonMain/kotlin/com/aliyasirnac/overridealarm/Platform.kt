package com.aliyasirnac.overridealarm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform