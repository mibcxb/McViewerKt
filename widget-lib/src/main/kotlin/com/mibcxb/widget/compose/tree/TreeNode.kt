package com.mibcxb.widget.compose.tree

import org.jetbrains.compose.resources.DrawableResource

abstract class TreeNode(
    override val icon: DrawableResource?,
    var expanded: Boolean = false,
    var selected: Boolean = false
) : TreeItem {
    open fun children(): List<TreeNode> {
        return emptyList()
    }
}