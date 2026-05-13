package com.mibcxb.widget.compose.file

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object FileStubNone : FileStub {
    override val path: String = ""
    override val name: String = ""
    override val extension: String = ""
    override val fileType: FileType = FileType.NAN
    override val length: Long = 0L
    override val lastModified: Long = 0L
    override val createdAt: Long = 0L
    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf()

    override fun refreshList(filter: FileStubFilter) {
        // nothing to do
    }

    override fun refreshStub(newStub: FileStub) {
        // nothing to do
    }

    override fun exists(): Boolean = false
    override fun isFile(): Boolean = false
}