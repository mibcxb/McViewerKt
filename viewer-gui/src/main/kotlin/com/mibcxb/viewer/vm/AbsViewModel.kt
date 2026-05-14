package com.mibcxb.viewer.vm

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.ViewModel
import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.viewer.log.LogApi
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerItem
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

abstract class AbsViewModel(protected val cacheApi: CacheApi = CacheSqlite()) : ViewModel(), LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    fun getThumbBuffer(curItem: ViewerItem): Buffer? {
        val dataBytes = getThumbnail(curItem) ?: return null
        return Buffer().write(dataBytes)
    }

    fun getThumbnail(curItem: ViewerItem): ByteArray? {
        if (!curItem.isImage) return null
        val curBytes = cacheApi.obtainCacheThumb(curItem.path.raw)
        if (curBytes != null) return curBytes
        val newBytes = when {
            curItem.isLocal -> {
                genThumbSkia(File(curItem.path.basePath))
            }
            curItem.isSamba -> {
                val data = curItem.getInputStream()?.use { it.readBytes() } ?: return null
                genThumbSkia(data, curItem.extension)
            }
            else -> return null
        }
        if (newBytes != null) {
            val flag = cacheApi.insertCacheThumb(curItem.path.raw, newBytes)
            logger.debug("insertCacheThumb: $flag, path: ${curItem.path.raw}, size: ${newBytes.size}")
        }
        return newBytes
    }

    private fun genThumbSkia(
        file: File,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? = genThumbSkia(file.readBytes(), file.extension, target, format, samplingMode, quality)

    private fun genThumbSkia(
        data: ByteArray,
        extension: String,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? = kotlin.runCatching {
        if (FileType.SVG.extensions.contains(extension.lowercase())) {
            SkiaUtils.svgThumb(data, target = target, format = format, quality = quality)
        } else {
            SkiaUtils.genThumb(data, target, format, samplingMode, quality)
        }
    }.onFailure { logger.error(logTag, it.message, it) }.getOrNull()

    fun getThumbBitmap(curItem: ViewerItem): ImageBitmap? {
        val dataBytes = getThumbnail(curItem) ?: return null
        return Image.makeFromEncoded(dataBytes).toComposeImageBitmap()
    }
}
