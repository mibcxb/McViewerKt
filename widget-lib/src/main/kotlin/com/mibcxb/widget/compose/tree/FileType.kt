package com.mibcxb.widget.compose.tree

enum class FileType(val extensions: Array<String> = emptyArray()) {
    NAN,
    DIR,
    JPG(arrayOf("jpg", "jpeg")),
    PNG(arrayOf("png"));
}