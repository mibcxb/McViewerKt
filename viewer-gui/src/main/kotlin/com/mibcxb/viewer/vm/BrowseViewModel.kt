package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.viewModelScope
import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileSortType
import com.mibcxb.widget.compose.tree.FileItem
import com.mibcxb.widget.compose.tree.FileTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.File
import java.io.FileFilter

class BrowseViewModel(cacheApi: CacheApi = CacheSqlite()) : AbsViewModel(cacheApi) {
    private val _fileTree = FileTree()
    val fileTree: FileTree get() = _fileTree

    private val _fileStub = mutableStateOf<FileStub>(FileStubNone)
    val fileStub: State<FileStub> get() = _fileStub

    private val _filePath = mutableStateOf("")
    val filePath: State<String> get() = _filePath

    private val _previewImageStub = mutableStateOf<FileStub>(FileStubNone)
    val previewImageStub: State<FileStub> get() = _previewImageStub

    private val _fileTypeList = mutableStateListOf(*(FileTypes.images + FileTypes.archives))
    val fileTypeList: SnapshotStateList<FileType> get() = _fileTypeList

    private val _searchName = mutableStateOf("")
    val searchName: State<String> get() = _searchName

    private val _fileSortType = mutableStateOf(FileSortType.Filename)
    val fileSortType: State<FileSortType> get() = _fileSortType

    private val _fileGridSize = mutableStateOf(FileGridSize.Middle)
    val fileGridSize: State<FileGridSize> get() = _fileGridSize

    private val fileFilter: FileFilter = FileFilter { file ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            file.isHidden -> false
            file.isFile -> extNames.contains(file.extension)
            file.isDirectory -> true
            else -> false
        }
    }

    private val _selectedItem = mutableStateOf<FileItem?>(null)
    val selectedItem: State<FileItem?> get() = _selectedItem

    fun initFileTree() {
        if (_fileTree.branches.isNotEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val rootList = File.listRoots().filter { it.canRead() }.map { FileItem(it) }
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
        changePreviewImageStub(File(stub.path))
    }

    fun doubleClickGridItem(stub: FileStub) {
        if (_fileStub.value != stub) {
            _fileStub.value = stub.apply {
                refreshList(fileFilter)
            }
            _filePath.value = stub.path
        }
    }

    fun changeFilePath(newPath: String) {
        if (_filePath.value != newPath) {
            _filePath.value = newPath
        }
    }

    fun changeSearchName(newName: String) {
        if (_searchName.value != newName) {
            _searchName.value = newName
        }
    }

    fun changeFileSortType(index: Int) {
        val sortType = FileSortType.entries.getOrNull(index) ?: return
        changeFileSortType(sortType)
    }

    fun changeFileSortType(newType: FileSortType) {
        if (_fileSortType.value != newType) {
            _fileSortType.value = newType
        }
    }

    fun changeFileGridSize(index: Int) {
        val sizeList = FileGridSize.entries.toList()
        if (index in sizeList.indices) {
            changeFileGridSize(sizeList[index])
        }
    }

    fun changeFileGridSize(newSize: FileGridSize) {
        if (_fileGridSize.value != newSize) {
            _fileGridSize.value = newSize
        }
    }

    fun removeSearchName() {
        changeSearchName("")
    }

    fun goToTargetPath() {
        goToTargetPath(_filePath.value)
    }

    fun goToTargetPath(newPath: String) {
//        logger.info("goToTargetPath: path='$newPath'")
        if (newPath.isNotBlank()) {
            val newFile = File(newPath)
            changeFileStub(newFile)
        }
    }

    fun goToParentPath() {
        val curStub = _fileStub.value
        if (curStub is FileStubImpl) {
            val newFile = curStub.file.parentFile
            if (newFile != null) {
                changeFileStub(newFile)
            }
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
}