package com.mibcxb.common.skia

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import kotlin.math.min


object SkiaUtils {
    fun genThumb(
        imageBytes: ByteArray,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? {
        // 1) 解码原图
        val src = Image.makeFromEncoded(imageBytes)
        val sw = src.width.toFloat()
        val sh = src.height.toFloat()
        val dw = target.width
        val dh = target.height
        // 2) 计算源区域与目标区域（保持比例）
        val scale = min(dw / sw, dh / sh)
        val outW = (sw * scale)
        val outH = (sh * scale)
        val dx = (dw - outW) / 2f // 居中放置
        val dy = (dh - outH) / 2f
        val srcRect = Rect.makeXYWH(0f, 0f, sw, sh)
        val dstRect = Rect.makeXYWH(dx, dy, outW, outH)
        // 3) 在目标画布上高质量绘制
        val surface = Surface.makeRasterN32Premul(target.width.toInt(), target.height.toInt()).apply {
            canvas.run {
                clear(0x00000000) // 透明背景
                val paint = Paint().apply { isAntiAlias = true } // 抗锯齿
                drawImageRect(src, srcRect, dstRect, samplingMode, paint, true)
            }
        }
        // 4) 导出缩略图
        return surface.makeImageSnapshot().encodeToData(format, quality)?.bytes
    }

    fun svgThumb(
        svgBytes: ByteArray,
        background: Color = Color.Transparent,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        quality: Int = 90
    ): ByteArray? {
        val svgDom = SVGDOM(Data.makeFromBytes(svgBytes))
        val surface = Surface.makeRasterN32Premul(target.width.toInt(), target.height.toInt()).apply {
            canvas.run {
                clear(0x00000000) // 透明背景
                if (background != Color.Transparent) {
                    drawRect(
                        Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat()),
                        Paint().apply { color = background.toArgb() })
                }
            }
            // 设置 SVG 容器尺寸
            svgDom.setContainerSize(width.toFloat(), height.toFloat())
            // 渲染
            svgDom.render(canvas)
        }
        return surface.makeImageSnapshot().encodeToData(format, quality)?.bytes
    }
}