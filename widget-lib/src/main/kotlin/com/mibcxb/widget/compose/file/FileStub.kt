package com.mibcxb.widget.compose.file

import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.io.FileFilter

interface FileStub {
    val file: File
    val path: String
    val name: String
    val extension: String
    val fileType: FileType
    val subFiles: SnapshotStateList<FileStub>
    val subCount: Int get() = subFiles.size

    fun refreshList() = refreshList { true }
    fun refreshList(filter: FileFilter)
}