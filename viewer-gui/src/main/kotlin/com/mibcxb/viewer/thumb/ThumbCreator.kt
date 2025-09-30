package com.mibcxb.viewer.thumb

import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.log.LogApi
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import kotlin.collections.toTypedArray

class ThumbCreator(private val cacheApi: CacheApi, private val thumbParam: ThumbParam = ThumbParam()) : LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    private val pendingSet = linkedSetOf<String>()
    private var processJob: Job? = null

    fun scan(root: File, types: Array<FileType> = FileTypes.images) {
        val extensions = types.flatMap { it.extensions.toList() }.toTypedArray()
        val filter = FileFilter {
            extensions.contains(it.extension.lowercase())
        }
        scan(root, filter, pendingSet)
    }

    private fun scan(file: File, filter: FileFilter, images: LinkedHashSet<String>) {
        if (!file.exists() || file.isHidden) {
            return
        }
        if (file.isFile && filter.accept(file)) {
            images += file.absolutePath
        } else if (file.isDirectory) {
            file.listFiles().forEach {
                scan(it, filter, images)
            }
        }
    }

    fun launch(scope: CoroutineScope, permits: Int = 5, listener: (Int, Int) -> Unit = { count, total -> }) {
        processJob?.cancel()
        processJob = scope.launch {
            val semaphore = Semaphore(permits)
            val pathList = pendingSet.toList()
            val iterator = pathList.iterator()
            val total = pathList.size
            var count = 0
            while (isActive && iterator.hasNext()) {
                semaphore.withPermit {
                    val filepath = iterator.next()
                    processImage(filepath)
                    listener(++count, total)
                }
            }
        }
    }

    fun cancel() {
        processJob?.cancel()
        processJob = null
    }

    private fun processImage(path: String) {
        kotlin.runCatching {
            val file = File(path)
            val target = thumbParam.targetSize
            val format = thumbParam.encodeFormat
            val samplingMode = thumbParam.samplingMode
            val quality = thumbParam.imageQuality
            if (FileType.SVG.extensions.contains(file.extension.lowercase())) {
                SkiaUtils.svgThumb(
                    svgBytes = file.readBytes(),
                    target = target,
                    format = format,
                    quality = quality
                )
            } else {
                SkiaUtils.genThumb(
                    imageBytes = file.readBytes(),
                    target = target,
                    format = format,
                    samplingMode = samplingMode,
                    quality = quality
                )
            }
        }.onFailure { logger.error(it.message, it) }
    }
}