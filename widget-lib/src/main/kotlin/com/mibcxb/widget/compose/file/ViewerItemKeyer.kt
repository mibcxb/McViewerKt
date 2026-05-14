package com.mibcxb.widget.compose.file

import coil3.key.Keyer
import coil3.request.Options

class ViewerItemKeyer : Keyer<ViewerItem> {
    override fun key(data: ViewerItem, options: Options): String = "viewer:${data.path.raw}"
}
