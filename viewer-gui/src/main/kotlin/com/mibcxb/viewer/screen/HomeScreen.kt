package com.mibcxb.viewer.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.vm.HomeViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.menu_about
import com.mibcxb.viewer_gui.generated.resources.menu_edit
import com.mibcxb.viewer_gui.generated.resources.menu_file
import com.mibcxb.widget.compose.Divider
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel { HomeViewModel() }) {
    val appRes = LocalAppRes.current
    Column(modifier = Modifier.fillMaxSize()) {
        MenuBar(this)
        Divider(appRes.dimen.dividerWidth)
        Content(this)
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
private fun Content(colScope: ColumnScope) = colScope.run {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.fillMaxWidth().weight(1.0f)) {
        Column(modifier = Modifier.weight(0.25f)) {
            // FileTreeView
            Divider(appRes.dimen.dividerWidth)
            Preview(this)
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Column(modifier = Modifier.weight(0.75f)) {

        }
    }
}

@Composable
private fun Preview(colScope: ColumnScope) = colScope.run {
}