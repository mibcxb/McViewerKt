package com.mibcxb.widget.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Divider(width: Dp = 1.dp, color: Color = Color.LightGray, vertical: Boolean = false) {
    if (vertical) {
        Box(modifier = Modifier.width(width).fillMaxHeight().background(color))
    }else{
        Box(modifier = Modifier.fillMaxWidth().height(width).background(color))
    }
}