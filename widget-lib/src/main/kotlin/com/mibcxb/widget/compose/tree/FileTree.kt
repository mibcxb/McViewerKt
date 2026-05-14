package com.mibcxb.widget.compose.tree

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.file.ViewerItem

@Stable
class FileTree(branches: List<ViewerItem> = emptyList()) {
    val branches: SnapshotStateList<ViewerItem> = mutableStateListOf<ViewerItem>().apply { addAll(branches) }
}