package com.mibcxb.viewer.screen

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.vm.ArchiveViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_left
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_jpg
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_png
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_svg
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemKeyer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ArchiveScreenView(
    cacheApi: CacheApi,
    vm: ArchiveViewModel = viewModel { ArchiveViewModel(cacheApi) },
    filepath: String = "",
    nav: NavController
) {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.fillMaxSize()) {
        val vScroll = rememberLazyListState()
        val hScroll = rememberScrollState()
        Column(modifier = Modifier.weight(0.225f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                    .padding(horizontal = appRes.dimen.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier.size(appRes.dimen.iconButtonSize)
                ) {
                    Image(
                        painterResource(Res.drawable.ic_arrow_circle_left),
                        contentDescription = null,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
            Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val subEntryList = remember { vm.subEntryList }
                LazyColumn(
                    state = vScroll,
                    contentPadding = PaddingValues(vertical = appRes.dimen.paddingPanel),
                    modifier = Modifier.wrapContentWidth().fillMaxHeight().horizontalScroll(hScroll)
                ) {
                    items(subEntryList, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(
                                    min = appRes.dimen.archiveItemHeightMin,
                                    max = appRes.dimen.archiveItemHeightMax
                                )
                                .combinedClickable(
                                    onClick = { vm.singleClickListItem(item) },
                                    onDoubleClick = { }
                                )
                        ) {
                            val iconSize = appRes.dimen.archiveItemIconSize
                            val hPadding = appRes.dimen.paddingPanel
                            val drawable: DrawableResource? = when (item.fileType) {
                                FileType.JPG -> Res.drawable.icon_filetype_jpg
                                FileType.PNG -> Res.drawable.icon_filetype_png
                                FileType.SVG -> Res.drawable.icon_filetype_svg
                                else -> null
                            }
                            if (drawable != null) {
                                Icon(
                                    painterResource(drawable),
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize)
                                )
                            } else {
                                Spacer(modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize))
                            }
                            Text(item.path.entryPath.ifEmpty { item.name }, modifier = Modifier.padding(end = hPadding).wrapContentWidth())
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(vScroll),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(hScroll),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Box(modifier = Modifier.weight(0.775f).fillMaxHeight()) {
            val subEntryItem by remember { vm.subEntryItem }
            if (subEntryItem != null) {
                val platformContext = LocalPlatformContext.current
                AsyncImage(
                    model = subEntryItem,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    imageLoader = ImageLoader.Builder(platformContext).components {
                        add(ViewerItemKeyer())
                        add(
                            DelegateFetcher.Factory(
                                source = DataSource.DISK,
                                getData = vm::getSubEntryData,
                                getMime = vm::getSubEntryMime
                            )
                        )
                    }.build(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}
