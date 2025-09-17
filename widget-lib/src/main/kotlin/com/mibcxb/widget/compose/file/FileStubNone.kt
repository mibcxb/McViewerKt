package com.mibcxb.widget.compose.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.FileFilter

object FileStubNone : FileStub {
    override val path: String = ""
    override val name: String = ""
    override val extension: String = ""
    override val fileType: FileType = FileType.NAN
    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf()

    override fun refreshList(filter: FileFilter) {
        // nothing to do
    }

    override fun refreshStub(newStub: FileStub) {
        // nothing to do
    }

    override fun exists(): Boolean = false
    override fun isFile(): Boolean = false
}