package com.misra.domain.model

object LyricFontSize {
    const val MinSp = 14f
    const val MaxSp = 22f
    const val DefaultSp = 16f

    fun coerce(value: Float): Float = value.coerceIn(MinSp, MaxSp)

    fun fraction(value: Float): Float =
        ((coerce(value) - MinSp) / (MaxSp - MinSp)).coerceIn(0f, 1f)

    fun fromFraction(fraction: Float): Float =
        MinSp + fraction.coerceIn(0f, 1f) * (MaxSp - MinSp)
}
