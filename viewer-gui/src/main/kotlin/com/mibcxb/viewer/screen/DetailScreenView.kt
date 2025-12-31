package com.mibcxb.viewer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.vm.DetailViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_left
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_right
import com.mibcxb.viewer_gui.generated.resources.ic_cancel
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubKeyer
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailScreenView(
    cacheApi: CacheApi,
    vm: DetailViewModel = viewModel { DetailViewModel(cacheApi) },
    filepath: String = "",
    nav: NavController
) {
    val appRes = LocalAppRes.current
    Box(modifier = Modifier.fillMaxSize().background(color = Color.LightGray)) {
        val filepath by remember { vm.filePath }
        AsyncImage(model = filepath, contentDescription = null, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = { vm.next() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.CenterEnd)
        ) {
            Image(
                painterResource(Res.drawable.ic_arrow_circle_right),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = { vm.prev() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.CenterStart)
        ) {
            Image(
                painterResource(Res.drawable.ic_arrow_circle_left),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = { nav.popBackStack() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.TopEnd)
        ) {
            Image(painterResource(Res.drawable.ic_cancel), contentDescription = null, modifier = Modifier.fillMaxSize())
        }

        val fileStub by remember { vm.parentStub }
        val showList by remember { vm.showList }
        if (showList && fileStub.exists() && fileStub.isDirectory()) {
            val platformContext = LocalPlatformContext.current
            LazyRow(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.DarkGray),
                contentPadding = PaddingValues(
                    horizontal = appRes.dimen.paddingLarge,
                    vertical = appRes.dimen.paddingPanel
                ),
                horizontalArrangement = Arrangement.spacedBy(appRes.dimen.paddingPanel)
            ) {
                items(fileStub.subFiles, key = { it.path }) { fileItem ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(appRes.dimen.paddingSmall)
                            .combinedClickable(
                                onClick = { vm.changeFilePath(fileItem.path) }
                            )) {
                        AsyncImage(
                            model = fileItem,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            imageLoader = ImageLoader.Builder(platformContext).components {
                                add(FileStubKeyer())
                                add(
                                    DelegateFetcher.Factory(
                                        source = DataSource.DISK,
                                        getData = { vm.getThumbBuffer(it as FileStub) },
                                        getMime = { "image/png" }
                                    ))
                            }.build(),
                            modifier = Modifier.size(appRes.dimen.detailPreviewWidth, appRes.dimen.detailPreviewHeight)
                        )
                        Text(
                            fileItem.name,
                            color = Color.White,
                            fontSize = appRes.dimen.detailTextSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = appRes.dimen.paddingSmall)
                                .widthIn(0.dp, appRes.dimen.detailPreviewWidth)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}