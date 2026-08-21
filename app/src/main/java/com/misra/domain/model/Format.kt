package com.misra.domain.model

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
