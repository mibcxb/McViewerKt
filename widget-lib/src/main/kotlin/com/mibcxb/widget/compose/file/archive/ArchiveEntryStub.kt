package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileType
import org.apache.commons.compress.archivers.ArchiveEntry
import kotlin.io.path.Path
import kotlin.io.path.name

abstract class ArchiveEntryStub(val archiveEntry: ArchiveEntry) {
    protected val archiveEntryPath = Path(archiveEntry.name)
    val dirLevel = archiveEntryPath.nameCount

    val path: String get() = archiveEntry.name
    val name: String get() = archiveEntryPath.name
    val extension: String get() = name.substringAfterLast(".")
    val fileType: FileType = if (archiveEntry.isDirectory) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }
    val length: Long get() = archiveEntry.size
    val lastModified: Long get() = archiveEntry.lastModifiedDate?.time ?: 0L
    val createdAt: Long get() = lastModified

    fun isDirectory(): Boolean = archiveEntry.isDirectory
    fun isFile(): Boolean = !archiveEntry.isDirectory

    companion object {
        fun getByPath(accessor: ArchiveAccessor, entryPath: String): ArchiveEntryStub? {
            return accessor.getEntryList().find { it.archiveEntry.name == entryPath }
        }
    }
}
