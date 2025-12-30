package com.mibcxb.viewer.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.vm.BrowseViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_cancel
import com.mibcxb.viewer_gui.generated.resources.ic_copy
import com.mibcxb.viewer_gui.generated.resources.ic_file_archive
import com.mibcxb.viewer_gui.generated.resources.ic_file_image
import com.mibcxb.viewer_gui.generated.resources.ic_folder
import com.mibcxb.viewer_gui.generated.resources.ic_folder_create
import com.mibcxb.viewer_gui.generated.resources.ic_folder_upward
import com.mibcxb.viewer_gui.generated.resources.ic_next
import com.mibcxb.viewer_gui.generated.resources.ic_refresh
import com.mibcxb.viewer_gui.generated.resources.ic_search
import com.mibcxb.viewer_gui.generated.resources.ic_settings
import com.mibcxb.viewer_gui.generated.resources.ic_sort
import com.mibcxb.viewer_gui.generated.resources.ic_zoom_in
import com.mibcxb.viewer_gui.generated.resources.ic_zoom_out
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_jpg
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_png
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_svg
import com.mibcxb.viewer_gui.generated.resources.item_large
import com.mibcxb.viewer_gui.generated.resources.item_middle
import com.mibcxb.viewer_gui.generated.resources.item_small
import com.mibcxb.viewer_gui.generated.resources.sort_type
import com.mibcxb.viewer_gui.generated.resources.text_create_folder
import com.mibcxb.viewer_gui.generated.resources.text_files
import com.mibcxb.viewer_gui.generated.resources.text_input_keyword
import com.mibcxb.viewer_gui.generated.resources.text_preview
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileGridView
import com.mibcxb.widget.compose.tree.FileTreeView
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreenViewNew(vm: BrowseViewModel = viewModel { BrowseViewModel() }, nav: NavController) {
    val appRes = LocalAppRes.current
    Column(modifier = Modifier.fillMaxSize()) {
        ToolbarView(
            vm,
            modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                .padding(horizontal = appRes.dimen.paddingLarge)
        )
        Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
        ContentView(
            vm,
            nav,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }

    LaunchedEffect(Unit) {
        vm.initFileTree()
    }
}

@Composable
private fun ToolbarView(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

@Composable
private fun FuncRowView(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { vm.goToParentPath() },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_folder_upward),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        IconButton(
            onClick = { vm.refreshCurrent() },
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel).size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier
                .padding(appRes.dimen.paddingPanel)
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
                .padding(horizontal = appRes.dimen.paddingPanel),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(Res.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
            val searchName by remember { vm.searchName }
            BasicTextField(
                value = searchName,
                onValueChange = { vm.changeSearchName(it) },
                modifier = Modifier.padding(horizontal = appRes.dimen.paddingSmall).weight(1f),
                maxLines = 1
            ) { innerTextField ->
                if (searchName.isEmpty()) {
                    Text(stringResource(Res.string.text_input_keyword))
                }
                innerTextField()
            }
            if (searchName.isNotEmpty()) {
                IconButton(
                    onClick = { vm.removeSearchName() },
                    modifier = Modifier.size(appRes.dimen.searchNameIcon)
                ) {
                    Image(
                        painterResource(Res.drawable.ic_cancel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(appRes.dimen.paddingPanel)
                .wrapContentWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
                .clickable {}
                .padding(horizontal = appRes.dimen.paddingPanel),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(Res.drawable.ic_folder_create),
                contentDescription = null,
                modifier = Modifier.padding(end = appRes.dimen.paddingSmall).wrapContentSize()
            )
            Text(stringResource(Res.string.text_create_folder))
        }
        val fileGridSize by remember { vm.fileGridSize }
        IconButton(
            onClick = { vm.changeFileGridSize(fileGridSize.ordinal - 1) },
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel).size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_zoom_out),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier
                .padding(
                    horizontal = appRes.dimen.paddingPanel,
                    vertical = appRes.dimen.paddingPanel
                )
                .wrapContentWidth()
                .fillMaxHeight()
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = appRes.dimen.cornerNormal,
                            bottomStart = appRes.dimen.cornerNormal
                        )
                    )
                    .let {
                        if (fileGridSize == FileGridSize.Small) {
                            it.background(
                                Color.LightGray,
                                RoundedCornerShape(
                                    topStart = appRes.dimen.cornerNormal,
                                    bottomStart = appRes.dimen.cornerNormal
                                )
                            )
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Small) }
                    .padding(start = appRes.dimen.paddingPanel, end = appRes.dimen.paddingSmall),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_small))
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .let {
                        if (fileGridSize == FileGridSize.Middle) {
                            it.background(Color.LightGray)
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Middle) }
                    .padding(horizontal = appRes.dimen.paddingSmall),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_middle))
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topEnd = appRes.dimen.cornerNormal,
                            bottomEnd = appRes.dimen.cornerNormal
                        )
                    )
                    .let {
                        if (fileGridSize == FileGridSize.Large) {
                            it.background(
                                Color.LightGray,
                                RoundedCornerShape(
                                    topEnd = appRes.dimen.cornerNormal,
                                    bottomEnd = appRes.dimen.cornerNormal
                                )
                            )
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Large) }
                    .padding(start = appRes.dimen.paddingSmall, end = appRes.dimen.paddingPanel),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_large))
            }
        }
        IconButton(
            onClick = { vm.changeFileGridSize(fileGridSize.ordinal + 1) },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_zoom_in),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier.padding(start = appRes.dimen.paddingLarge).wrapContentWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            val sortType by remember { vm.fileSortType }
            val sortTypes = stringArrayResource(Res.array.sort_type)
            Text(sortTypes[sortType.ordinal])
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                sortTypes.forEachIndexed { index, label ->
                    DropdownMenuItem(onClick = { vm.changeFileSortType(index) }) {
                        Text(text = label)
                    }
                }
            }
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Image(
                    painterResource(Res.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}

@Composable
private fun PathRowView(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filePath by remember { vm.filePath }
        if (filePath.isNotBlank()) {
            val segments = mutableListOf<String>()
            val path = Paths.get(filePath).normalize()
            if (path.root != null) {
                segments.add(path.root.toString())
            }
            if (path.nameCount > 0) {
                path.forEach { segments.add(it.toString()) }
            }
            LazyRow(modifier = Modifier.weight(1f)) {
                itemsIndexed(segments) { index, segment ->
                    PathSegmentView(index, segment, index == segments.lastIndex) {
//                        val segList = segments.subList(0, it + 1)
//                        val newPath = if (segList.size > 1) {
//                            Paths.get(segList.first(), *segList.subList(1, segList.size).toTypedArray())
//                        } else {
//                            Paths.get(segList.first(), File.separator)
//                        }
                        val newPath = segments.subList(0, it + 1).joinToString(File.separator)
                        vm.goToTargetPath(newPath)
                    }
                }
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Image(
                    painterResource(Res.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}

@Composable
private fun PathSegmentView(index: Int, segment: String, last: Boolean = false, onClick: (Int) -> Unit = {}) {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = segment,
            modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .clickable { onClick(index) }.padding(appRes.dimen.paddingSmall)
        )
        if (!last) {
            Image(
                painterResource(Res.drawable.ic_next),
                contentDescription = null,
                colorFilter = ColorFilter.tint(appRes.color.pathArrowTint),
                modifier = Modifier.padding(horizontal = appRes.dimen.paddingSmall).size(appRes.dimen.pathArrowSize)
            )
        }
    }
}

@Composable
private fun ContentView(vm: BrowseViewModel, nav: NavController, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(0.25f).fillMaxHeight()) {
            val fileTree = remember { vm.fileTree }
            FileTreeView(
                fileTree = fileTree,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onSingleClick = { vm.singleClickTreeItem(it) },
                onDoubleClick = {
                    when {
                        FileTypes.isImage(it.fileType) -> nav.navigate(DetailScreen(it.path))
                        FileTypes.isArchive(it.fileType) -> nav.navigate(ArchiveScreen(it.path))
                        FileTypes.isDir(it.fileType) -> vm.doubleClickTreeItem(it)
                    }
                })
            Divider(appRes.dimen.dividerWidth)
            Preview(vm, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f))
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Column(modifier = Modifier.weight(0.75f).fillMaxHeight()) {
            val fileStub by remember { vm.fileStub }
            if (fileStub.exists() && fileStub.isDirectory()) {
                FuncRowView(
                    vm,
                    modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                        .padding(horizontal = appRes.dimen.paddingLarge)
                )
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                PathRowView(
                    vm,
                    modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                        .padding(horizontal = appRes.dimen.paddingLarge)
                )
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                val fileGridSize by remember { vm.fileGridSize }
                val searchName by remember { vm.searchName }
                FileGridView(
                    fileStub = fileStub,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    itemSize = fileGridSize,
                    onSingleClick = { vm.singleClickGridItem(it) },
                    onDoubleClick = {
                        when {
                            FileTypes.isImage(it.fileType) -> nav.navigate(DetailScreen(it.path))
                            FileTypes.isArchive(it.fileType) -> nav.navigate(ArchiveScreen(it.path))
                            FileTypes.isDir(it.fileType) -> vm.doubleClickGridItem(it)
                        }
                    },
                    cacheLoader = { vm.getThumbBuffer(it) },
                    errorLoader = {
                        when {
                            it.isDirectory() -> Res.drawable.ic_folder
                            it.isArchive() -> Res.drawable.ic_file_archive
                            else -> Res.drawable.ic_file_image
                        }
                    },
                    imageLoader = {
                        when (it.fileType) {
                            FileType.JPG -> Res.drawable.icon_filetype_jpg
                            FileType.PNG -> Res.drawable.icon_filetype_png
                            FileType.SVG -> Res.drawable.icon_filetype_svg
                            else -> null
                        }
                    }
                ) {
                    it.name.contains(searchName, ignoreCase = true)
                }
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(end = appRes.dimen.paddingPanel).fillMaxWidth().height(24.dp)
                ) {
                    Text(stringResource(Res.string.text_files, fileStub.subCount))
                }
            }
        }
    }
}

@Composable
private fun Preview(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Column(modifier = modifier) {
        Text(
            stringResource(Res.string.text_preview),
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel, top = appRes.dimen.paddingSmall)
                .wrapContentSize()
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1.0f).background(appRes.color.imagePreviewBackground)) {
            val previewImageStub by remember { vm.previewImageStub }
            if (previewImageStub.exists() && previewImageStub.isFile()) {
                AsyncImage(
                    model = previewImageStub.path,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(appRes.dimen.paddingSmall).fillMaxWidth().wrapContentHeight()
        ) {

        }
    }
}