package com.misra.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

class AppAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtElapsed: Long = 0L
    private var pausedAtElapsed: Long = 0L
    private var pausedAccumulated: Long = 0L

    var isRecording: Boolean = false
        private set
    var isPaused: Boolean = false
        private set

    val elapsedMs: Long
        get() {
            if (!isRecording && recorder == null) return 0L
            val now = if (isPaused) pausedAtElapsed else SystemClock.elapsedRealtime()
            return (now - startedAtElapsed - pausedAccumulated).coerceAtLeast(0L)
        }

    fun start(output: File) {
        cancel()
        output.parentFile?.mkdirs()
        if (output.exists()) output.delete()
        val mediaRecorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = output
        startedAtElapsed = SystemClock.elapsedRealtime()
        pausedAccumulated = 0L
        isPaused = false
        isRecording = true
    }

    fun pause() {
        val media = recorder ?: return
        if (!isRecording || isPaused) return
        media.pause()
        pausedAtElapsed = SystemClock.elapsedRealtime()
        isPaused = true
    }

    fun resume() {
        val media = recorder ?: return
        if (!isRecording || !isPaused) return
        media.resume()
        pausedAccumulated += SystemClock.elapsedRealtime() - pausedAtElapsed
        isPaused = false
    }

    fun stop(): File {
        val file = outputFile ?: error("No active recording")
        finalizeRecorder()
        return file
    }

    fun cancel() {
        val file = outputFile
        finalizeRecorder()
        file?.delete()
    }

    private fun finalizeRecorder() {
        runCatching {
            recorder?.apply {
                if (isRecording) {
                    stop()
                }
                reset()
                release()
            }
        }
        recorder = null
        outputFile = null
        isRecording = false
        isPaused = false
        startedAtElapsed = 0L
        pausedAtElapsed = 0L
        pausedAccumulated = 0L
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
