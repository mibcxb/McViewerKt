package com.mibcxb.widget.compose.file

import java.io.FileFilter

object FileTypes {
    val images: Array<FileType> = arrayOf(FileType.JPG, FileType.PNG, FileType.SVG)
    val imageExtensions: Array<String> = images.flatMap { it.extensions.toList() }.toTypedArray()

    val imageFilter: FileFilter = FileFilter { file ->
        file.exists() && file.isFile && !file.isHidden && imageExtensions.contains(file.extension)
    }

    fun isImage(fileType: FileType): Boolean = images.contains(fileType)

    fun isDir(fileType: FileType): Boolean = fileType == FileType.DIR
}