package com.misra.data

import com.misra.domain.model.SongDocument
import com.misra.domain.model.SongSummary
import com.misra.domain.model.isBlankDraft
import com.misra.domain.model.toSummary
import com.misra.domain.workspace.emptySong
import com.misra.domain.workspace.normalizedVertical
import kotlinx.serialization.json.Json
import java.io.File

val MisraJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

interface SongStore {
    suspend fun list(): List<SongSummary>
    suspend fun load(id: String): SongDocument?
    suspend fun save(document: SongDocument)
    suspend fun create(now: Long): SongDocument
    suspend fun delete(id: String)
}

class FileSongStore(
    private val songsDirectory: File,
    private val legacyFile: File? = null
) : SongStore {

    override suspend fun list(): List<SongSummary> {
        migrateLegacyIfNeeded()
        if (!songsDirectory.exists()) return emptyList()
        return songsDirectory
            .listFiles { file -> file.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                val document = decode(file)?.normalizedVertical() ?: return@mapNotNull null
                if (document.isBlankDraft()) {
                    file.delete()
                    null
                } else {
                    document.toSummary()
                }
            }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun load(id: String): SongDocument? {
        migrateLegacyIfNeeded()
        return decode(fileFor(id))?.normalizedVertical()
    }

    override suspend fun save(document: SongDocument) {
        if (document.isBlankDraft()) {
            delete(document.id)
            return
        }
        songsDirectory.mkdirs()
        val file = fileFor(document.id)
        val temp = File(songsDirectory, "${document.id}.json.tmp")
        temp.writeText(MisraJson.encodeToString(SongDocument.serializer(), document))
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    override suspend fun create(now: Long): SongDocument = emptySong(now)

    override suspend fun delete(id: String) {
        fileFor(id).delete()
    }

    private fun fileFor(id: String): File = File(songsDirectory, "$id.json")

    private fun decode(file: File): SongDocument? {
        if (!file.exists()) return null
        return runCatching {
            MisraJson.decodeFromString(SongDocument.serializer(), file.readText())
        }.getOrNull()
    }

    private fun migrateLegacyIfNeeded() {
        val legacy = legacyFile ?: return
        if (!legacy.exists()) return
        songsDirectory.mkdirs()
        val alreadyMigrated = songsDirectory.listFiles { file -> file.extension == "json" }?.isNotEmpty() == true
        if (alreadyMigrated) return
        val document = decode(legacy)?.normalizedVertical() ?: return
        File(songsDirectory, "${document.id}.json").writeText(
            MisraJson.encodeToString(SongDocument.serializer(), document)
        )
    }
}
