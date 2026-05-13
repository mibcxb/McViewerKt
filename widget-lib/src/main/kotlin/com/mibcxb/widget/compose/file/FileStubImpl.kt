package com.mibcxb.widget.compose.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class FileStubImpl(val file: File) : FileStub {
    override val path: String = file.canonicalPath
    override val name: String get() = file.name.ifEmpty { path }

    override val extension: String get() = file.extension.lowercase()

    override val fileType: FileType = if (file.isDirectory) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }

    override val length: Long get() = file.length()
    override val lastModified: Long get() = file.lastModified()
    override val createdAt: Long by lazy {
        try {
            Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
        } catch (_: Exception) {
            file.lastModified()
        }
    }

    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf<FileStub>()

    override fun refreshList(filter: FileStubFilter) {
        synchronized(subFiles) {
            if (file.isDirectory) {
                subFiles.clear()
                val children = file.listFiles()
                if (children != null) {
                    val nodeList = children
                        .filterNot { it.isHidden }
                        .mapNotNull { FileStubImpl(it) }
                        .filter { filter(it) }
                    subFiles.addAll(nodeList)
                }
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

    override fun exists(): Boolean = file.exists()
    override fun isFile(): Boolean = file.isFile
}