package com.mibcxb.widget.compose.file

import java.io.FileFilter

object FileTypes {

    fun isDir(fileType: FileType): Boolean = fileType == FileType.DIR

    val images: Array<FileType> = arrayOf(FileType.JPG, FileType.PNG/*, FileType.SVG*/)
    val imageExtensions: Array<String> = images.flatMap { it.extensions.toList() }.toTypedArray()

    val imageFilter: FileFilter = FileFilter { file ->
        file.exists() && file.isFile && !file.isHidden && imageExtensions.contains(file.extension)
    }

    fun isImage(fileType: FileType): Boolean = images.contains(fileType)

    val archives: Array<FileType> = arrayOf(FileType.ZIP, FileType.SevenZ)
    val archiveExtensions: Array<String> = archives.flatMap { it.extensions.toList() }.toTypedArray()

    fun isArchive(fileType: FileType): Boolean = archives.contains(fileType)
}