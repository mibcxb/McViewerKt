package com.mibcxb.widget.compose.tree

interface TreeItem {
    val id: String
    val name: String

    val children: List<TreeItem>
}