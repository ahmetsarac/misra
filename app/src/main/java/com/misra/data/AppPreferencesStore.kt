package com.misra.data

import android.content.Context
import androidx.core.content.edit
import com.misra.domain.model.LyricFontSize

class AppPreferencesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lyricFontSizeSp(): Float =
        LyricFontSize.coerce(prefs.getFloat(KEY_LYRIC_FONT_SIZE, LyricFontSize.DefaultSp))

    fun setLyricFontSizeSp(value: Float) {
        prefs.edit {
            putFloat(KEY_LYRIC_FONT_SIZE, LyricFontSize.coerce(value))
        }
    }

    private companion object {
        const val PREFS_NAME = "misra_prefs"
        const val KEY_LYRIC_FONT_SIZE = "lyric_font_size_sp"
    }
}
