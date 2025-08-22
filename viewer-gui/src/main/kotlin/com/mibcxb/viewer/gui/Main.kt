package com.mibcxb.viewer.gui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.mibcxb.viewer.app.AppNav
import com.mibcxb.viewer.app.AppResImpl
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        title = stringResource(Res.string.app_name),
        state = WindowState(position = WindowPosition.Aligned(Alignment.Center), width = 1024.dp, height = 768.dp),
        onCloseRequest = ::exitApplication
    ) {
        CompositionLocalProvider(LocalAppRes provides AppResImpl()) {
            AppNav()
        }
    }
}