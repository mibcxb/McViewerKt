package com.mibcxb.widget.compose.tree

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

@Stable
class FileTree(branches: List<FileItem> = emptyList()) {
    val branches: SnapshotStateList<FileItem> = mutableStateListOf<FileItem>().apply { addAll(branches) }
}