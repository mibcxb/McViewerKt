package com.mibcxb.viewer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.vm.DetailViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_left
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_right
import com.mibcxb.viewer_gui.generated.resources.ic_cancel
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailScreenView(vm: DetailViewModel = viewModel { DetailViewModel() }, filepath: String = "", nav: NavController) {
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
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}