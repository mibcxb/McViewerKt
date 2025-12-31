package com.mibcxb.viewer.vm

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.ViewModel
import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.viewer.log.LogApi
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileType
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.collections.contains

abstract class AbsViewModel(protected val cacheApi: CacheApi = CacheSqlite()) : ViewModel(), LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    fun getThumbBuffer(curStub: FileStub): Buffer? {
        val dataBytes = getThumbnail(curStub) ?: return null
        return Buffer().write(dataBytes)
    }

    fun getThumbnail(curStub: FileStub): ByteArray? {
        if (curStub !is FileStubImpl || !curStub.isImage()) {
            return null
        }
        val curBytes = cacheApi.obtainCacheThumb(curStub.path)
        if (curBytes != null) {
            return curBytes
        }
        val newBytes = genThumbSkia(curStub.file)
        if (newBytes != null) {
            val flag = cacheApi.insertCacheThumb(curStub.path, newBytes)
            logger.debug("insertCacheThumb: $flag, path: ${curStub.path}, size: ${newBytes.size}")
        }
        return newBytes
    }

    private fun genThumbSkia(
        file: File,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? = kotlin.runCatching {
        if (FileType.SVG.extensions.contains(file.extension.lowercase())) {
            SkiaUtils.svgThumb(file.readBytes(), target = target, format = format, quality = quality)
        } else {
            SkiaUtils.genThumb(file.readBytes(), target, format, samplingMode, quality)
        }
    }.onFailure { logger.error(logTag, it.message, it) }.getOrNull()

    private fun genThumbnail(curStub: FileStub): FileStub? {
        if (curStub !is FileStubImpl || !curStub.isImage()) {
            return null
        }

        val path = curStub.path
        val file = curStub.file
        if (cacheApi.isPathExists(path)) {
            return null
        }

        val dataBytes = genThumbSkia(file)
        if (dataBytes != null) {
            if (cacheApi.insertCacheThumb(path, dataBytes)) {
                return FileStubImpl(file)
            }
        }
        return null
    }


    fun getThumbBitmap(curStub: FileStub): ImageBitmap? {
        val dataBytes = getThumbnail(curStub) ?: return null
        return Image.makeFromEncoded(dataBytes).toComposeImageBitmap()
    }
}