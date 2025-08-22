package com.mibcxb.widget.compose.tree

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import com.mibcxb.widget.widget_lib.generated.resources.folder_opened
import org.jetbrains.compose.resources.DrawableResource
import java.io.File

@Composable
fun <T : TreeNode> TreeView(
    rootList: List<T>,
    modifier: Modifier = Modifier,
    iconLoader: ((T) -> DrawableResource?)? = null
) {
    LazyColumn(modifier = modifier) {}
}

@Composable
fun FileTreeView(
    fileList: List<File>,
    modifier: Modifier = Modifier,
    iconLoader: ((FileNode) -> DrawableResource?)? = {
        when (it.fileType) {
            FileType.NAN -> null
            FileType.DIR -> if (it.expanded) Res.drawable.folder_opened else Res.drawable.folder_normal
            else -> Res.drawable.file
        }
    }
) {
    val rootList = fileList.map { FileNode(it) }
    TreeView(rootList = rootList, modifier = modifier, iconLoader = iconLoader)
}