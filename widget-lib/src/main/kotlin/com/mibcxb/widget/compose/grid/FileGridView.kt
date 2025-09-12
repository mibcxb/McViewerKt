package com.mibcxb.widget.compose.grid

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.mibcxb.widget.compose.coil.FileStubFetcher
import com.mibcxb.widget.compose.coil.FileStubKeyer
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.widget_lib.generated.resources.Res
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
    cacheLoader: (FileStub) -> ByteArray? = { null },
    imageLoader: (FileStub) -> DrawableResource? = { null }
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp, 144.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onSingleClick(fileItem) },
                        onDoubleClick = { onDoubleClick(fileItem) })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val drawable = imageLoader(fileItem)
                        ?: if (fileStub.isDirectory()) Res.drawable.folder_normal else Res.drawable.file_unknown
                    val platformContext = LocalPlatformContext.current
                    AsyncImage(
                        model = fileItem,
                        contentDescription = null,
                        error = painterResource(drawable),
                        placeholder = painterResource(drawable),
                        contentScale = ContentScale.Fit,
                        imageLoader = ImageLoader.Builder(platformContext).components {
                            add(FileStubKeyer())
                            add(
                                FileStubFetcher.Factory(
                                    repo = cacheLoader,
                                    mime = { "image/png" }
                                ))
                        }.build(),
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                    )
                    Text(
                        fileItem.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().weight(1f)
                    )
                }
            }
        }
    }
}