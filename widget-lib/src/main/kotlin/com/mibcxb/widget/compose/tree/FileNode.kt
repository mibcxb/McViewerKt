package com.mibcxb.widget.compose.tree

import org.jetbrains.compose.resources.DrawableResource
import java.io.File

class FileNode(val file: File, icon: DrawableResource? = null) :
    TreeNode(icon = icon, expanded = false) {
    private val _children = mutableListOf<FileNode>()
    private val _lowerExt: String = file.extension.lowercase()
    private val _fileType: FileType by lazy {
        if (file.isDirectory) {
            FileType.DIR
        } else {
            FileType.entries.find { it.extensions.contains(_lowerExt) } ?: FileType.NAN
        }
    }

    val extension: String get() = _lowerExt
    val fileType: FileType get() = _fileType

    override val name: String get() = file.name

    override fun children(): List<TreeNode> {
        if (file.isDirectory) {
            if (_children.isEmpty()) {
                file.listFiles().mapNotNull { FileNode(it) }
            }
            return _children
        }
        return super.children()
    }
}