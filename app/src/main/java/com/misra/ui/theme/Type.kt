package com.misra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.misra.domain.model.LyricFontSize

fun lyricTextStyle(sizeSp: Float): TextStyle {
    val size = LyricFontSize.coerce(sizeSp)
    return TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = (size * (25f / 16f)).sp,
        letterSpacing = 0.12.sp
    )
}

val LyricTextStyle = lyricTextStyle(LyricFontSize.DefaultSp)

val LocalLyricFontSize = compositionLocalOf { LyricFontSize.DefaultSp }

val Typography = Typography(
    bodyLarge = LyricTextStyle,
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)
