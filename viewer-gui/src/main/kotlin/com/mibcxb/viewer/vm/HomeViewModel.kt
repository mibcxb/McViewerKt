package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.viewModelScope
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.tree.FileItem
import com.mibcxb.widget.compose.tree.FileTree
import com.mibcxb.widget.compose.file.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterQuality
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File
import java.io.FileFilter
import kotlin.math.min

class HomeViewModel(val cacheApi: CacheApi = CacheSqlite()) : AbsViewModel() {
    private val _fileTree = FileTree()
    val fileTree: FileTree get() = _fileTree

    private val _fileStub = mutableStateOf<FileStub>(FileStubNone)
    val fileStub: State<FileStub> get() = _fileStub

    private val _filePath = mutableStateOf("")
    val filePath: State<String> get() = _filePath

    private val _previewImageStub = mutableStateOf<FileStub>(FileStubNone)
    val previewImageStub: State<FileStub> get() = _previewImageStub

    private val _fileTypeList = mutableStateListOf(FileType.JPG, FileType.PNG)
    val fileTypeList: SnapshotStateList<FileType> get() = _fileTypeList

    private val fileFilter: FileFilter = FileFilter { file ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            file.isHidden -> false
            file.isFile -> extNames.contains(file.extension)
            file.isDirectory -> true
            else -> false
        }
    }

    private var thumbJob: Job? = null

    private val _selectedItem = mutableStateOf<FileItem?>(null)
    val selectedItem: State<FileItem?> get() = _selectedItem

    fun initFileTree() {
        val rootList = File.listRoots().map { FileItem(it) }
        _fileTree.branches.apply {
            clear()
            addAll(rootList)
        }
        val fileItem = rootList.firstOrNull()
        if (fileItem != null) {
            _selectedItem.value = fileItem
            _fileStub.value = FileStubImpl(fileItem.file).apply { refreshList(fileFilter) }
            _filePath.value = fileItem.path
        }
    }

    fun singleClickTreeItem(item: FileItem) {
        if (_selectedItem.value != item) {
            _selectedItem.value = item
        }
        changeFileStub(item.file)
        changePreviewImageStub(item.file)
    }

    fun doubleClickTreeItem(item: FileItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList(fileFilter)
        }
    }

    fun singleClickGridItem(stub: FileStub) {
        changePreviewImageStub(stub.file)
    }

    fun doubleClickGridItem(stub: FileStub) {
        if (_fileStub.value != stub) {
            _fileStub.value = stub.apply {
                refreshList(fileFilter)
                restartThumbJob(this)
            }
            _filePath.value = stub.path
        }
    }

    fun changeFilePath(newPath: String) {
        if (_filePath.value != newPath) {
            _filePath.value = newPath
        }
    }

    fun goToTargetPath() {
        val newPath = _filePath.value
        val newFile = File(newPath)
        changeFileStub(newFile)
    }

    fun goToParentPath() {
        val curStub = _fileStub.value
        val newFile = curStub.file.parentFile
        if (newFile != null) {
            changeFileStub(newFile)
        }
    }

    fun refreshCurrent() {
        _fileStub.value.refreshList(fileFilter)
    }

    private fun changeFileStub(newFile: File) {
        if (!newFile.exists() || !newFile.isDirectory) {
            return
        }
        if (_fileStub.value.path == newFile.canonicalPath) {
            return
        }
        _fileStub.value = FileStubImpl(newFile).apply {
            refreshList(fileFilter)
            restartThumbJob(this)
        }
        _filePath.value = newFile.canonicalPath

    }

    private fun changePreviewImageStub(newFile: File) {
        if (!newFile.exists() || !newFile.isFile) {
            return
        }
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        if (!extNames.contains(newFile.extension)) {
            return
        }
        if (_previewImageStub.value.path == newFile.canonicalPath) {
            return
        }
        _previewImageStub.value = FileStubImpl(newFile)
    }

    private fun restartThumbJob(rootStub: FileStub) {
        thumbJob?.cancel()
        thumbJob = viewModelScope.launch(Dispatchers.IO) {
            val iterator = rootStub.subFiles.toList().iterator()
            while (isActive && iterator.hasNext()) {
                val curStub = iterator.next()
                val newStub = genThumbnail(curStub)
                if (newStub != null) {
                    rootStub.refreshStub(newStub)
                }
            }
        }
    }

    private fun genThumbnail(curStub: FileStub): FileStub? {
        if (!curStub.isImage()) {
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

    private fun genThumbSkia(
        file: File,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        quality: Int = 90,
    ): ByteArray? = kotlin.runCatching {
        // 1) 解码原图
        val fileBytes = file.readBytes()
        val src = Image.makeFromEncoded(fileBytes)
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
                drawImageRect(src, srcRect, dstRect, SamplingMode.DEFAULT, paint, true)
            }
        }
        // 4) 导出缩略图
        return surface.makeImageSnapshot().encodeToData(format, quality)?.bytes
    }.onFailure { logger.error(logTag, it.message, it) }.getOrNull()

//    fun getThumbnail(curStub: FileStub): ImageBitmap? {
//        if (!curStub.isImage()) {
//            return null
//        }
//        val dataBytes = cacheApi.obtainCacheThumb(curStub.path)
//        if (dataBytes != null) {
//            return Image.makeFromEncoded(dataBytes).toComposeImageBitmap()
//        }
//        return null
//    }

    fun getThumbnail(curStub: FileStub): ByteArray? {
        if (!curStub.isImage()) {
            return null
        }
        return cacheApi.obtainCacheThumb(curStub.path)
    }
}