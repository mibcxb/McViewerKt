package com.mibcxb.viewer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mibcxb.viewer.vm.DetailViewModel

@Composable
fun DetailScreenView(vm: DetailViewModel = viewModel { DetailViewModel() }, filepath: String = "") {
    Box(modifier = Modifier.fillMaxSize().background(color = Color.LightGray)) {
        val filepath by remember { vm.filepath }
        AsyncImage(model = filepath, contentDescription = null, modifier = Modifier.fillMaxSize())
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}