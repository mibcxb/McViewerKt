package com.mibcxb.widget.compose.file

import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.FileFilter

interface FileStub {
    val path: String
    val name: String
    val extension: String
    val fileType: FileType
    val subFiles: SnapshotStateList<FileStub>
    val subCount: Int get() = subFiles.size

    fun refreshList() = refreshList { true }
    fun refreshList(filter: FileFilter)

    fun refreshStub(newStub: FileStub)

    fun exists(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean = FileTypes.isDir(fileType)
    fun isImage(): Boolean = FileTypes.isImage(fileType)
    fun isArchive(): Boolean = FileTypes.isArchive(fileType)
}