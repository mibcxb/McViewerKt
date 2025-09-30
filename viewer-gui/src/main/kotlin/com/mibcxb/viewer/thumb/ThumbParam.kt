package com.mibcxb.viewer.thumb

import androidx.compose.ui.geometry.Size
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.SamplingMode

data class ThumbParam(
    val targetSize: Size = Size(160f, 120f),
    val encodeFormat: EncodedImageFormat = EncodedImageFormat.PNG,
    val samplingMode: SamplingMode = SamplingMode.DEFAULT,
    val imageQuality: Int = 90
)
