package com.mibcxb.widget.compose.tree

import org.jetbrains.compose.resources.DrawableResource

interface TreeItem {
    val name: String
    val icon: DrawableResource?
}