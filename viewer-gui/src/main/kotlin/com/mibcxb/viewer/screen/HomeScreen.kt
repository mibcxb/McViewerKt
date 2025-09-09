package com.mibcxb.viewer.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.vm.HomeViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_folder_create
import com.mibcxb.viewer_gui.generated.resources.ic_folder_delete
import com.mibcxb.viewer_gui.generated.resources.ic_folder_upward
import com.mibcxb.viewer_gui.generated.resources.ic_refresh
import com.mibcxb.viewer_gui.generated.resources.ic_return
import com.mibcxb.viewer_gui.generated.resources.menu_about
import com.mibcxb.viewer_gui.generated.resources.menu_edit
import com.mibcxb.viewer_gui.generated.resources.menu_file
import com.mibcxb.viewer_gui.generated.resources.text_files
import com.mibcxb.viewer_gui.generated.resources.text_preview
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.grid.FileGridView
import com.mibcxb.widget.compose.tree.FileTreeView
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel { HomeViewModel() }) {
    val appRes = LocalAppRes.current
    Column(modifier = Modifier.fillMaxSize()) {
        MenuBar(this)
        Divider(appRes.dimen.dividerWidth)
        Content(this, vm)
    }

    LaunchedEffect(Unit) {
        vm.initFileTree()
    }
}

@Composable
private fun MenuBar(colScope: ColumnScope) = colScope.run {
    val appRes = LocalAppRes.current
    Row(
        modifier = Modifier.fillMaxWidth().height(appRes.dimen.headerHeight)
            .padding(horizontal = appRes.dimen.paddingPanel)
    ) {
        val menuItemWidth = appRes.dimen.menuItemWidth
        val menuTextSize = appRes.dimen.menuTextSize
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(menuItemWidth).fillMaxHeight().clickable {}) {
            Text(
                stringResource(Res.string.menu_file),
                fontSize = menuTextSize,
                textAlign = TextAlign.Center
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(menuItemWidth).fillMaxHeight().clickable {}) {
            Text(
                stringResource(Res.string.menu_edit),
                fontSize = menuTextSize,
                textAlign = TextAlign.Center
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(menuItemWidth).fillMaxHeight().clickable {}) {
            Text(
                stringResource(Res.string.menu_about),
                fontSize = menuTextSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Content(colScope: ColumnScope, vm: HomeViewModel) = colScope.run {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.fillMaxWidth().weight(1.0f)) {
        Column(modifier = Modifier.weight(0.25f).fillMaxHeight()) {
            val fileTree = remember { vm.fileTree }
            FileTreeView(
                fileTree = fileTree,
                modifier = Modifier.fillMaxWidth().weight(0.7f),
                onSingleClick = { vm.singleClickTreeItem(it) },
                onDoubleClick = { vm.doubleClickTreeItem(it) })
            Divider(appRes.dimen.dividerWidth)
            Preview(this, vm)
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Column(modifier = Modifier.weight(0.75f).fillMaxHeight()) {
            val fileStub by remember { vm.fileStub }
            if (fileStub.file.exists() && fileStub.file.isDirectory) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                        .padding(horizontal = appRes.dimen.paddingPanel)
                        .background(appRes.color.filepathBackground)
                ) {
                    val filepath by remember { vm.filePath }
                    BasicTextField(
                        value = filepath,
                        onValueChange = vm::changeFilePath,
                        modifier = Modifier.weight(1f).height(24.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { vm.goToTargetPath() },
                        modifier = Modifier.padding(start = appRes.dimen.paddingSmall).size(appRes.dimen.iconButtonSize)
                    ) {
                        Image(painterResource(Res.drawable.ic_return), contentDescription = null)
                    }
//                    IconButton(
//                        onClick = { vm.refreshCurrent() },
//                        modifier = Modifier.padding(start = appRes.dimen.paddingSmall).size(appRes.dimen.iconButtonSize)
//                    ) {
//                        Image(painterResource(Res.drawable.ic_refresh), contentDescription = null)
//                    }
                    IconButton(
                        onClick = { vm.goToParentPath() },
                        modifier = Modifier.padding(start = appRes.dimen.paddingSmall).size(appRes.dimen.iconButtonSize)
                    ) {
                        Image(painterResource(Res.drawable.ic_folder_upward), contentDescription = null)
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.padding(start = appRes.dimen.paddingSmall).size(appRes.dimen.iconButtonSize)
                    ) {
                        Image(painterResource(Res.drawable.ic_folder_create), contentDescription = null)
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.padding(start = appRes.dimen.paddingSmall).size(appRes.dimen.iconButtonSize)
                    ) {
                        Image(painterResource(Res.drawable.ic_folder_delete), contentDescription = null)
                    }
                }
                Divider(appRes.dimen.dividerWidth)
                FileGridView(
                    fileStub = fileStub,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onSingleClick = { vm.singleClickGridItem(it) },
                    onDoubleClick = { vm.doubleClickGridItem(it) },
                    iconLoader = { vm.getThumbnail(it) })
                Divider(appRes.dimen.dividerWidth)
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
private fun Preview(colScope: ColumnScope, vm: HomeViewModel) = colScope.run {
    val appRes = LocalAppRes.current
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f)) {
        Text(
            stringResource(Res.string.text_preview),
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel, top = appRes.dimen.paddingSmall)
                .wrapContentSize()
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1.0f).background(appRes.color.imagePreviewBackground)) {
            val previewImageStub by remember { vm.previewImageStub }
            if (previewImageStub.file.exists() && previewImageStub.file.isFile) {
                AsyncImage(
                    model = previewImageStub.file,
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