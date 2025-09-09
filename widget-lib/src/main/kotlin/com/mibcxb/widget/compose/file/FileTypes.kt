package com.mibcxb.widget.compose.file

object FileTypes {
    val imageTypes: Array<FileType> = arrayOf(FileType.JPG, FileType.PNG)
    fun isImage(fileType: FileType): Boolean = imageTypes.contains(fileType)

    fun isDir(fileType: FileType):Boolean = fileType == FileType.DIR
}