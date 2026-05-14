package com.mibcxb.widget.compose.file

import java.io.File
import java.io.InputStream

class LocalSource : ViewerSource {
    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val dir = File(path.basePath)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filterNot { it.isHidden }
            ?.map { ViewerPath.create(it) }
            ?: emptyList()
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val file = File(path.basePath)
        return if (file.isFile && file.canRead()) file.inputStream() else null
    }

    override fun exists(path: ViewerPath): Boolean = File(path.basePath).exists()
}
