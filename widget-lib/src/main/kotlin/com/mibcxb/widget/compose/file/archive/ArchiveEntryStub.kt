package com.mibcxb.widget.compose.file.archive

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileType
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.FileFilter
import kotlin.collections.contains
import kotlin.io.path.Path
import kotlin.io.path.name

abstract class ArchiveEntryStub(val archiveEntry: ArchiveEntry) : FileStub {
    protected val archiveEntryPath = Path(archiveEntry.name)
    val dirLevel = archiveEntryPath.nameCount

    override val path: String
        get() = archiveEntry.name
    override val name: String
        get() = archiveEntryPath.name

    override val extension: String
        get() = name.substringAfterLast(".")
    override val fileType: FileType = if (archiveEntry.isDirectory) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }
    override val subFiles: SnapshotStateList<FileStub>
        get() = mutableStateListOf<FileStub>()


    override fun refreshList(filter: FileFilter) {
        // do nothing
    }

    override fun refreshStub(newStub: FileStub) {
        // do nothing
    }

    override fun exists(): Boolean = true

    override fun isFile(): Boolean = !archiveEntry.isDirectory
}