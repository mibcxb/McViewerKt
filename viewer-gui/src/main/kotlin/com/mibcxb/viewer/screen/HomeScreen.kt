package com.mibcxb.viewer.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.vm.HomeViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.menu_about
import com.mibcxb.viewer_gui.generated.resources.menu_edit
import com.mibcxb.viewer_gui.generated.resources.menu_file
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.grid.FileGridView
import com.mibcxb.widget.compose.tree.FileTreeView
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
                        onValueChange = {  },
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        singleLine = true
                    )
                }
                Divider(appRes.dimen.dividerWidth)
                FileGridView(fileStub,modifier = Modifier.fillMaxWidth().weight(1f))
                Divider(appRes.dimen.dividerWidth)
                Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {}
            }

        }
    }
}

@Composable
private fun Preview(colScope: ColumnScope, vm: HomeViewModel) = colScope.run {
    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3f))
}