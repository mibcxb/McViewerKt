package com.mibcxb.viewer.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppColorDef : AppColor {
    override val borderNormal: Color = Color.LightGray
    override val filepathBackground: Color = Color.White
    override val imagePreviewBackground: Color = Color.LightGray
}

object AppDimenDef : AppDimen {
    override val paddingSmall: Dp = 4.dp
    override val paddingPanel: Dp = 8.dp
    override val paddingLarge: Dp = 16.dp

    override val dividerWidth: Dp = 1.dp

    override val headerHeight: Dp = 24.dp
    override val footerHeight: Dp = 24.dp

    override val menuItemWidth: Dp = 36.dp
    override val menuTextSize: TextUnit = 12.sp

    override val iconButtonSize: Dp = 24.dp
}