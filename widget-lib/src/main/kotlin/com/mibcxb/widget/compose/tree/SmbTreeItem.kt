package com.mibcxb.widget.compose.tree

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.protocol.commons.EnumWithValue
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.samba.SmbSession

@Stable
class SmbTreeItem(
    val host: String,
    val share: String,
    val remotePath: String,
    val session: SmbSession,
    val isRoot: Boolean = false
) : TreeItem, Expandable, Selectable {

    override val children: SnapshotStateList<SmbTreeItem> = mutableStateListOf()

    override val id: String get() = "smb:$host:$share:$remotePath"

    override val name: String get() = if (isRoot) "$share@$host" else remotePath.substringAfterLast("/")

    private var _expanded by mutableStateOf(false)
    override val expanded: Boolean get() = _expanded

    private var _selected by mutableStateOf(false)
    override val selected: Boolean get() = _selected

    val fileType: FileType get() = FileType.DIR

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

    fun refreshList() {
        val entries = session.listFiles(remotePath)
        val dirEntries = entries
            .filter { it.fileName != "." && it.fileName != ".." }
            .filter {
                EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
            }
        val childItems = dirEntries.map { entry ->
            val childPath = if (remotePath.isEmpty() || remotePath == "/") {
                entry.fileName
            } else {
                "$remotePath/${entry.fileName}"
            }
            SmbTreeItem(host, share, childPath, session)
        }
        children.clear()
        children.addAll(childItems)
    }
}
