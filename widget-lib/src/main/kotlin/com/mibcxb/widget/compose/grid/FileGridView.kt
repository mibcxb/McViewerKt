package com.mibcxb.widget.compose.grid

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file
import com.mibcxb.widget.widget_lib.generated.resources.file_unknown
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun FileGridView(
    fileStub: FileStub,
    modifier: Modifier = Modifier,
    onSingleClick: (FileStub) -> Unit = {},
    onDoubleClick: (FileStub) -> Unit = {},
    iconLoader: ((FileStub) -> DrawableResource?) = {
        when (it.fileType) {
            FileType.NAN -> null
            FileType.DIR -> Res.drawable.folder_normal
            else -> Res.drawable.file
        }
    }
) {
    if (fileStub.fileType != FileType.DIR) {
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier
    ) {
        items(fileStub.subFiles, key = { it.path }) { fileItem ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.size(96.dp, 128.dp)
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val drawable = iconLoader(fileItem)
                    ?: if (fileStub.fileType == FileType.DIR) Res.drawable.folder_normal else Res.drawable.file_unknown
                Image(
                    painterResource(drawable),
                    fileItem.path,
                    modifier = Modifier.padding(vertical = 8.dp).size(80.dp, 60.dp),
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    fileItem.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        }
    }
}