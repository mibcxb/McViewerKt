package com.mibcxb.widget.compose.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.io.FileFilter

class FileStubImpl(override val file: File) : FileStub {
    override val path: String = file.canonicalPath
    override val name: String get() = file.name.ifEmpty { path }

    override val extension: String get() = file.extension.lowercase()

    override val fileType: FileType = if (file.isDirectory) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }

    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf<FileStub>()

    override fun refreshList(filter: FileFilter) {
        synchronized(subFiles) {
            if (file.isDirectory) {
                val nodeList = file.listFiles(filter).mapNotNull { FileStubImpl(it) }
                subFiles.clear()
                subFiles.addAll(nodeList)
            }
        }
    }

    override fun refreshStub(newStub: FileStub) {
        synchronized(subFiles) {
            val curStub = subFiles.find { it.path == newStub.path }
            if (curStub != null) {
                val index = subFiles.indexOf(curStub)
                subFiles[index] = newStub
            }
        }
    }
}