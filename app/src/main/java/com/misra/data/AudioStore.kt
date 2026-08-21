package com.misra.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

data class StoredAudio(
    val id: String,
    val name: String,
    val durationMs: Long,
    val file: File
)

interface AudioStore {
    suspend fun importFromUri(uri: Uri, fallbackName: String): StoredAudio
    suspend fun saveRecording(tempFile: File, name: String): StoredAudio
    fun resolve(audioId: String): File?
    suspend fun delete(audioId: String)
}

class FileAudioStore(
    private val context: Context,
    private val directory: File = File(context.filesDir, "audio")
) : AudioStore {

    override suspend fun importFromUri(uri: Uri, fallbackName: String): StoredAudio {
        directory.mkdirs()
        val displayName = queryDisplayName(uri) ?: fallbackName
        val extension = extensionOf(displayName).ifBlank { "m4a" }
        val id = UUID.randomUUID().toString()
        val target = File(directory, "$id.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read audio")
        return StoredAudio(
            id = id,
            name = displayName.substringBeforeLast('.').ifBlank { fallbackName },
            durationMs = probeDuration(target),
            file = target
        )
    }

    override suspend fun saveRecording(tempFile: File, name: String): StoredAudio {
        directory.mkdirs()
        val id = UUID.randomUUID().toString()
        val target = File(directory, "$id.m4a")
        if (tempFile.absolutePath != target.absolutePath) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
        return StoredAudio(
            id = id,
            name = name,
            durationMs = probeDuration(target),
            file = target
        )
    }

    override fun resolve(audioId: String): File? {
        if (!directory.exists()) return null
        return directory.listFiles()?.firstOrNull { it.nameWithoutExtension == audioId }
    }

    override suspend fun delete(audioId: String) {
        resolve(audioId)?.delete()
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun extensionOf(name: String): String {
        val index = name.lastIndexOf('.')
        return if (index >= 0 && index < name.length - 1) name.substring(index + 1) else ""
    }
}

fun probeDuration(file: File): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: RuntimeException) {
        0L
    } finally {
        retriever.release()
    }
}
