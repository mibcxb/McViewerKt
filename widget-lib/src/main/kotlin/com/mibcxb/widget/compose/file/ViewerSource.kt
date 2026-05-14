package com.mibcxb.widget.compose.file

import java.io.InputStream

interface ViewerSource {
    fun listChildren(path: ViewerPath): List<ViewerPath>
    fun getInputStream(path: ViewerPath): InputStream?
    fun exists(path: ViewerPath): Boolean
}
