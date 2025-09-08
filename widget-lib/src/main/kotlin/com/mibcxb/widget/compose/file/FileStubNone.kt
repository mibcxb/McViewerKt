package com.mibcxb.widget.compose.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.io.FileFilter

object FileStubNone: FileStub {
    override val file: File = File("")
    override val path: String=""
    override val name: String=""
    override val extension: String=""
    override val fileType: FileType= FileType.NAN
    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf()

    override fun refreshList(filter: FileFilter) {
        // nothing to do
    }
}