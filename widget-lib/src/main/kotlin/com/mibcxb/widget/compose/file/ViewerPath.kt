package com.mibcxb.widget.compose.file

import androidx.compose.runtime.Stable
import java.io.File

@Stable
class ViewerPath private constructor(
    val raw: String,
    val scheme: Scheme,
    val basePath: String,
    val entryPath: String,
    val archiveLayers: Int = 0,
    val name: String,
    val extension: String,
    val fileType: FileType,
    val length: Long,
    val lastModified: Long,
    val createdAt: Long
) {
    enum class Scheme { File, Samba }

    val isArchiveEntry get() = archiveLayers > 0
    val isLocal get() = scheme == Scheme.File
    val isSamba get() = scheme == Scheme.Samba
    val isFile get() = !FileTypes.isDir(fileType)
    val isDirectory get() = FileTypes.isDir(fileType)

    val parentPath: String by lazy {
        if (isArchiveEntry) {
            val lastSep = raw.lastIndexOf("!/")
            if (lastSep >= 0) raw.substring(0, lastSep) else raw
        } else {
            when (scheme) {
                Scheme.File -> File(basePath).parentFile?.canonicalPath ?: basePath
                Scheme.Samba -> {
                    val idx = basePath.lastIndexOf('/')
                    if (idx > 1) basePath.substring(0, idx) else basePath
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is ViewerPath && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw

    companion object {
        fun parse(raw: String): ViewerPath {
            val archiveSepIndex = raw.indexOf("!/")

            if (archiveSepIndex >= 0) {
                val base = raw.substring(0, archiveSepIndex)
                val entry = raw.substring(archiveSepIndex + 2)
                val basePath = parse(base)
                val internalName = File(entry).name
                val ext = internalName.substringAfterLast(".", "").lowercase()
                val fileType = if (entry.endsWith("/") || entry.isEmpty()) {
                    FileType.DIR
                } else {
                    FileType.entries.find { it.extensions.contains(ext) } ?: FileType.NAN
                }
                return ViewerPath(
                    raw = raw,
                    scheme = basePath.scheme,
                    basePath = base,
                    entryPath = entry.trimEnd('/'),
                    archiveLayers = basePath.archiveLayers + 1,
                    name = internalName.ifEmpty { entry.trimEnd('/') },
                    extension = ext,
                    fileType = fileType,
                    length = 0L,
                    lastModified = 0L,
                    createdAt = 0L
                )
            }

            return when {
                raw.startsWith("//") -> parseSamba(raw)
                raw.length >= 2 && raw[1] == ':' -> parseFile(File(raw))
                raw.startsWith("/") -> parseFile(File(raw))
                else -> parseFile(File(raw))
            }
        }

        private fun parseSamba(raw: String): ViewerPath {
            val cleaned = raw.removePrefix("//")
            val parts = cleaned.split("/", "\\").filter { it.isNotEmpty() }
            val host = parts.getOrElse(0) { "" }
            val share = parts.getOrElse(1) { "" }
            val remotePath = parts.drop(2).joinToString("/")

            val name = if (remotePath.isEmpty()) share else remotePath.substringAfterLast("/")
            val ext = name.substringAfterLast(".", "").lowercase()
            val fileType = FileType.DIR

            return ViewerPath(
                raw = raw,
                scheme = Scheme.Samba,
                basePath = raw,
                entryPath = "",
                archiveLayers = 0,
                name = name,
                extension = ext,
                fileType = fileType,
                length = 0L,
                lastModified = 0L,
                createdAt = 0L
            )
        }

        private fun parseFile(file: File): ViewerPath {
            val canonical = file.canonicalPath
            val isDir = file.isDirectory
            val fileType = if (isDir) FileType.DIR else {
                val ext = file.extension.lowercase()
                FileType.entries.find { it.extensions.contains(ext) } ?: FileType.NAN
            }
            return ViewerPath(
                raw = canonical,
                scheme = Scheme.File,
                basePath = canonical,
                entryPath = "",
                archiveLayers = 0,
                name = file.name.ifEmpty { canonical },
                extension = file.extension.lowercase(),
                fileType = fileType,
                length = if (isDir) 0L else file.length(),
                lastModified = file.lastModified(),
                createdAt = try {
                    java.nio.file.Files.readAttributes(
                        file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java
                    ).creationTime().toMillis()
                } catch (_: Exception) { file.lastModified() }
            )
        }

        fun create(file: File): ViewerPath = parseFile(file)

        fun create(host: String, share: String, remotePath: String): ViewerPath {
            return parseSamba(buildSmbString(host, share, remotePath))
        }

        fun createArchived(base: ViewerPath, archiveEntryName: String): ViewerPath {
            val raw = "${base.raw}!/$archiveEntryName"
            return parse(raw)
        }

        fun buildSmbString(host: String, share: String, remotePath: String): String {
            val cleanPath = remotePath.trim('/')
            return if (cleanPath.isEmpty()) "//$host/$share" else "//$host/$share/$cleanPath"
        }
    }
}
