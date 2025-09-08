package com.mibcxb.widget.compose.tree

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import com.mibcxb.widget.widget_lib.generated.resources.folder_opened
import com.mibcxb.widget.widget_lib.generated.resources.image
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun <T : TreeItem> TreeView(
    rootList: List<T>,
    modifier: Modifier = Modifier,
    onSingleClick: (T) -> Unit = {},
    onDoubleClick: (T) -> Unit = {},
    iconLoader: ((T) -> DrawableResource?)? = null
) {
    LazyColumn(modifier = modifier.horizontalScroll(rememberScrollState())) {
        items(rootList, key = { it.id }) {
            TreeNodeView(it, 0, onSingleClick, onDoubleClick, iconLoader)
        }
    }
}

@Composable
private fun <T : TreeItem> TreeNodeView(
    nodeItem: T,
    level: Int = 0,
    onSingleClick: (T) -> Unit = {},
    onDoubleClick: (T) -> Unit = {},
    iconLoader: ((T) -> DrawableResource?)? = null
) {
    val whiteStart = 16.dp * level
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = whiteStart).heightIn(min = 16.dp, max = 24.dp).combinedClickable(
            onClick = { onSingleClick(nodeItem) },
            onDoubleClick = { onDoubleClick(nodeItem) })
    ) {
        val iconSize = 20.dp
        val hPadding = 8.dp
        val drawable: DrawableResource? = iconLoader?.invoke(nodeItem)
        if (drawable != null) {
            Icon(
                painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize)
            )
        } else {
            Spacer(modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize))
        }
        Text(nodeItem.name, modifier = Modifier.padding(end = hPadding))
    }
    if (nodeItem is Expandable) {
        if (nodeItem.expanded) {
            nodeItem.children.forEach {
                TreeNodeView(it as T, level + 1, onSingleClick, onDoubleClick, iconLoader)
            }
        }
    }
}

@Composable
fun FileTreeView(
    fileTree: FileTree,
    modifier: Modifier = Modifier,
    onSingleClick: (FileItem) -> Unit = {},
    onDoubleClick: (FileItem) -> Unit = {},
    iconLoader: ((FileItem) -> DrawableResource?)? = {
        when (it.fileType) {
            FileType.NAN -> null
            FileType.DIR -> if (it.expanded) Res.drawable.folder_opened else Res.drawable.folder_normal
            else -> Res.drawable.file
        }
    }
) {
    TreeView(
        rootList = fileTree.branches,
        modifier = modifier,
        onSingleClick = onSingleClick,
        onDoubleClick = onDoubleClick
    ) {
        if (iconLoader != null) {
            iconLoader(it)
        } else {
            when (it.fileType) {
                FileType.DIR -> if (it.expanded) Res.drawable.folder_opened else Res.drawable.folder_normal
                FileType.JPG -> Res.drawable.image
                FileType.PNG -> Res.drawable.image
                else -> null
            }
        }
    }
}