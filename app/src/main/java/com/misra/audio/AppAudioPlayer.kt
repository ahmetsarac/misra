package com.misra.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

class AppAudioPlayer {

    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    var onComplete: (() -> Unit)? = null

    val isPlaying: Boolean get() = player?.isPlaying == true
    val positionMs: Long get() = player?.currentPosition?.toLong() ?: 0L
    val durationMs: Long get() = player?.duration?.takeIf { it > 0 }?.toLong() ?: 0L

    fun ensure(file: File) {
        if (currentFile == file && player != null) return
        reset()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(file.absolutePath)
            setOnCompletionListener { onComplete?.invoke() }
            prepare()
        }
        currentFile = file
    }

    fun play(file: File, fromMs: Long = 0L) {
        ensure(file)
        val media = player ?: return
        val duration = media.duration
        val startAt = when {
            fromMs > 0L -> fromMs.toInt()
            duration > 0 && media.currentPosition >= duration - 40 -> 0
            else -> media.currentPosition
        }
        media.seekTo(startAt)
        media.start()
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun seek(positionMs: Long) {
        player?.seekTo(positionMs.toInt().coerceAtLeast(0))
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        player?.setVolume(v, v)
    }

    fun reset() {
        runCatching {
            player?.reset()
            player?.release()
        }
        player = null
        currentFile = null
    }
}
