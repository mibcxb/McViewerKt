package com.mibcxb.widget.compose.file

import androidx.compose.runtime.snapshots.SnapshotStateList

typealias FileStubFilter = (FileStub) -> Boolean

interface FileStub {
    val path: String
    val name: String
    val extension: String
    val fileType: FileType
    val length: Long
    val lastModified: Long
    val createdAt: Long
    val subFiles: SnapshotStateList<FileStub>
    val subCount: Int get() = subFiles.size

    fun refreshList() = refreshList { true }
    fun refreshList(filter: FileStubFilter)

    fun refreshStub(newStub: FileStub)

    fun exists(): Boolean
    fun isFile(): Boolean
    fun isDirectory(): Boolean = FileTypes.isDir(fileType)
    fun isImage(): Boolean = FileTypes.isImage(fileType)
    fun isArchive(): Boolean = FileTypes.isArchive(fileType)
}