package com.mibcxb.viewer.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

interface AppDimen {
    val paddingSmall: Dp
    val paddingPanel: Dp
    val paddingLarge: Dp

    val dividerWidth: Dp

    val headerHeight: Dp
    val footerHeight: Dp

    val menuItemWidth: Dp
    val menuTextSize: TextUnit

    val iconButtonSize: Dp
}