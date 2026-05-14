package com.mibcxb.widget.compose.file

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.tree.Expandable
import com.mibcxb.widget.compose.tree.Selectable
import com.mibcxb.widget.compose.tree.TreeItem
import java.io.InputStream

typealias ViewerItemFilter = (ViewerItem) -> Boolean

@Stable
class ViewerItem(
    val path: ViewerPath,
    internal var source: ViewerSource? = null
) : TreeItem, Expandable, Selectable {

    override val id: String get() = path.raw
    override val name: String get() = path.name
    override val children: SnapshotStateList<ViewerItem> = mutableStateListOf()

    private var _expanded by mutableStateOf(false)
    override val expanded: Boolean get() = _expanded

    private var _selected by mutableStateOf(false)
    override val selected: Boolean get() = _selected

    fun setExpanded(enabled: Boolean) {
        if (_expanded != enabled) {
            _expanded = enabled
        }
    }

    fun setSelected(enabled: Boolean) {
        if (_selected != enabled) {
            _selected = enabled
        }
    }

    val extension: String get() = path.extension
    val fileType: FileType get() = path.fileType
    val length: Long get() = path.length
    val lastModified: Long get() = path.lastModified
    val createdAt: Long get() = path.createdAt
    val isDirectory get() = path.isDirectory
    val isFile get() = path.isFile
    val isLocal get() = path.isLocal
    val isSamba get() = path.isSamba
    val isArchiveEntry get() = path.isArchiveEntry
    val isImage get() = FileTypes.isImage(path.fileType)
    val isArchive get() = FileTypes.isArchive(path.fileType)

    fun refreshList(filter: ViewerItemFilter = { true }) {
        source?.let { src ->
            val paths = src.listChildren(path)
            children.clear()
            children.addAll(paths.map { ViewerItem(it, source) }.filter(filter))
        }
    }

    fun getInputStream(): InputStream? = source?.getInputStream(path)

    fun exists(): Boolean = source?.exists(path) ?: false
}
