package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileStub

fun interface ArchiveAccessorFactory {
    fun createArchiveAccessor(fileStub: FileStub): ArchiveAccessor?
}