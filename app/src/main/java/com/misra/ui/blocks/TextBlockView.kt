package com.misra.ui.blocks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.misra.R
import com.misra.domain.model.BlockPayload
import com.misra.ui.theme.LocalLyricFontSize
import com.misra.ui.theme.lyricTextStyle
import com.misra.ui.workspace.CursorRange

@Composable
fun TextBlockView(
    payload: BlockPayload.Text,
    restoreFocusGen: Int,
    isRestoreTarget: Boolean,
    fillsCanvas: Boolean,
    cursor: CursorRange,
    onSelect: () -> Unit,
    onTextChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    onBackspaceAtStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val fontSize = LocalLyricFontSize.current
    val focusRequester = remember { FocusRequester() }
    var field by remember {
        mutableStateOf(
            TextFieldValue(
                text = payload.content,
                selection = TextRange(
                    cursor.start.coerceIn(0, payload.content.length),
                    cursor.end.coerceIn(0, payload.content.length)
                )
            )
        )
    }

    LaunchedEffect(payload.content) {
        if (field.text != payload.content) {
            val length = payload.content.length
            field = TextFieldValue(
                text = payload.content,
                selection = TextRange(
                    field.selection.start.coerceIn(0, length),
                    field.selection.end.coerceIn(0, length)
                )
            )
        }
    }

    LaunchedEffect(restoreFocusGen) {
        if (isRestoreTarget && restoreFocusGen > 0) {
            val length = field.text.length
            field = field.copy(
                selection = TextRange(
                    cursor.start.coerceIn(0, length),
                    cursor.end.coerceIn(0, length)
                )
            )
            runCatching { focusRequester.requestFocus() }
        }
    }

    BasicTextField(
        value = field,
        onValueChange = { next ->
            field = next
            if (next.text != payload.content) onTextChange(next.text)
            onCursorChange(next.selection.start, next.selection.end)
        },
        textStyle = lyricTextStyle(fontSize).copy(color = ink),
        cursorBrush = SolidColor(accent),
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillsCanvas) Modifier.heightIn(min = 420.dp) else Modifier.heightIn(min = 28.dp))
            .padding(start = 28.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
                if (focus.isFocused) onSelect()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    field.selection.start == 0 &&
                    field.selection.end == 0
                ) {
                    onBackspaceAtStart()
                    true
                } else {
                    false
                }
            },
        decorationBox = { inner ->
            if (field.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.write_a_lyric),
                    style = lyricTextStyle(fontSize),
                    color = ink.copy(alpha = 0.32f)
                )
            }
            inner()
        }
    )
}
