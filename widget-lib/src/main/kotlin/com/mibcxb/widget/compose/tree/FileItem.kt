package com.mibcxb.widget.compose.tree

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.file.FileType
import java.io.File
import java.io.FileFilter

@Stable
class FileItem(file:File) :  TreeItem, Expandable, Selectable {
    var file by mutableStateOf(file)
      private set
    override val children: SnapshotStateList<FileItem> = mutableStateListOf<FileItem>()

    val path: String get() = file.canonicalPath
    override val id: String get() = path
    override val name: String get() = file.name.ifEmpty { path }

    private var expandable by mutableStateOf(false)
    override val expanded: Boolean get() = expandable

    private var selectable by mutableStateOf(false)
    override val selected: Boolean get() = selectable

    val extension: String get() = file.extension.lowercase()
    val fileType: FileType
        get() = if (file.isDirectory) {
            FileType.DIR
        } else {
            FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
        }

    fun refreshFile(newFile: File) {
        if (file != newFile) {
            file = newFile
            children.clear()
        }
    }

    fun refreshList() = refreshList { true }

    fun refreshList(filter: FileFilter) {
        if (file.isDirectory) {
            val nodeList = file.listFiles(filter).mapNotNull { FileItem(it) }
            children.clear()
            children.addAll(nodeList)
        }
    }

    fun setExpanded(enabled: Boolean) {
        if (expandable != enabled) {
            expandable = enabled
        }
    }

    fun setSelected(enabled: Boolean) {
        if (selectable != enabled) {
            selectable = enabled
        }
    }
}