package com.mibcxb.widget.compose.file

enum class FileType(val extensions: Array<String> = emptyArray()) {
    NAN,
    DIR,
    JPG(arrayOf("jpg", "jpeg")),
    PNG(arrayOf("png"));
}