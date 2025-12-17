package com.mibcxb.widget.compose.grid

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.file.FileStubKeyer
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file_unknown
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import okio.Buffer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class FileSortType {
    Filename, FileLength, CreateTime, LastModified
}

enum class FileGridSize {
    Small, Middle, Large
}

@Composable
fun FileGridView(
    fileStub: FileStub,
    modifier: Modifier = Modifier,
    sortType: FileSortType = FileSortType.Filename,
    itemSize: FileGridSize = FileGridSize.Middle,
    onSingleClick: (FileStub) -> Unit = {},
    onDoubleClick: (FileStub) -> Unit = {},
    cacheLoader: (FileStub) -> Buffer? = { null },
    errorLoader: (FileStub) -> DrawableResource? = { null },
    imageLoader: (FileStub) -> DrawableResource? = { null },
    fileFilter: (FileStub) -> Boolean = { true }
) {
    if (fileStub.fileType != FileType.DIR) {
        return
    }
    val itemWidth = when(itemSize) {
        FileGridSize.Small -> 96.dp
        FileGridSize.Large -> 144.dp
        else -> 120.dp
    }
    val itemHeight = when(itemSize) {
        FileGridSize.Small -> 128.dp
        FileGridSize.Large -> 192.dp
        else -> 160.dp
    }
    val fontSize = when(itemSize) {
        FileGridSize.Small -> 14.sp
        FileGridSize.Large -> 16.sp
        else -> 16.sp
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(itemWidth),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(fileStub.subFiles.filter(fileFilter), key = { it.path }) { fileItem ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(itemWidth, itemHeight)
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
                    val fallback = errorLoader(fileItem)
                        ?: if (fileStub.isDirectory()) Res.drawable.folder_normal else Res.drawable.file_unknown
                    val platformContext = LocalPlatformContext.current
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                        AsyncImage(
                            model = fileItem,
                            contentDescription = null,
                            error = painterResource(fallback),
                            placeholder = painterResource(fallback),
                            contentScale = ContentScale.Fit,
                            imageLoader = ImageLoader.Builder(platformContext).components {
                                add(FileStubKeyer())
                                add(
                                    DelegateFetcher.Factory(
                                        source = DataSource.DISK,
                                        getData = cacheLoader,
                                        getMime = { "image/png" }
                                    ))
                            }.build(),
                            modifier = Modifier.fillMaxSize()
                        )
                        val fileTypeImage = imageLoader(fileItem)
                        if (fileTypeImage != null) {
                            Image(
                                painterResource(fileTypeImage),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                            )
                        }
                    }
                    Text(
                        fileItem.name,
                        fontSize = fontSize,
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