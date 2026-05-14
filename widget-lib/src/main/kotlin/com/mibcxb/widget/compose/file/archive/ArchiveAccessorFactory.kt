package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileType

fun interface ArchiveAccessorFactory {
    fun createArchiveAccessor(fileType: FileType, filePath: String): ArchiveAccessor?
}
