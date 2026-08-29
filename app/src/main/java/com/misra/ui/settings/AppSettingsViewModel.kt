package com.misra.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.misra.data.AppPreferencesStore
import com.misra.domain.model.LyricFontSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AppPreferencesStore(application)

    private val _lyricFontSizeSp = MutableStateFlow(store.lyricFontSizeSp())
    val lyricFontSizeSp: StateFlow<Float> = _lyricFontSizeSp.asStateFlow()

    fun setLyricFontSize(sizeSp: Float) {
        val next = LyricFontSize.coerce(sizeSp)
        if (next == _lyricFontSizeSp.value) return
        _lyricFontSizeSp.value = next
        store.setLyricFontSizeSp(next)
    }
}
