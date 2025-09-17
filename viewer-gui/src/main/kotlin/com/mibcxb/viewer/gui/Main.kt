package com.mibcxb.viewer.gui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.mibcxb.viewer_gui.generated.resources.oppo_sans
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    val appTypography = InterTypography()
    Window(
        title = stringResource(Res.string.app_name),
        state = WindowState(position = WindowPosition.Aligned(Alignment.Center), width = 1024.dp, height = 768.dp),
        onCloseRequest = ::exitApplication
    ) {
        CompositionLocalProvider(LocalAppRes provides AppResImpl()) {
            MaterialTheme(typography = appTypography) {
                AppNav()
            }
        }
    }
}

@Composable
private fun InterTypography(): Typography {
    val interFont = FontFamily(
        Font(resource = Res.font.oppo_sans, weight = FontWeight.Normal),
        Font(resource = Res.font.oppo_sans, weight = FontWeight.Medium),
        Font(resource = Res.font.oppo_sans, weight = FontWeight.Bold),
        // 可选：斜体
        Font(resource = Res.font.oppo_sans, style = FontStyle.Italic, weight = FontWeight.Normal),
    )
    return Typography(defaultFontFamily = interFont)
}