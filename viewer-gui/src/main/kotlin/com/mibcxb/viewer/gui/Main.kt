package com.mibcxb.viewer.gui

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        title = "McViewer - 1.0.0",
        state = WindowState(position = WindowPosition.Aligned(Alignment.Center), width = 640.dp, height = 480.dp),
        onCloseRequest = ::exitApplication
    ) {
    }
}