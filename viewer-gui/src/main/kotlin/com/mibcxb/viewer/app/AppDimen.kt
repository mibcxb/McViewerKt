package com.mibcxb.viewer.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

interface AppDimen {
    val paddingSmall: Dp
    val paddingPanel: Dp
    val paddingLarge: Dp
    val paddingGiant: Dp

    val cornerNormal: Dp
    val dividerWidth: Dp
    val borderWidth: Dp

    val headerHeight: Dp
    val footerHeight: Dp
    val functionHeight: Dp

    val menuItemWidth: Dp
    val menuTextSize: TextUnit

    val iconButtonSize: Dp

    val searchNameIcon: Dp

    val pathArrowSize: Dp

    val detailIconSize: Dp
    val detailPreviewWidth: Dp
    val detailPreviewHeight: Dp
    val detailTextSize: TextUnit
}