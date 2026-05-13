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
import com.mibcxb.widget.compose.file.FileStubFilter
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.samba.SmbFileStub
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileSortType
import com.mibcxb.widget.compose.tree.FileItem
import com.mibcxb.widget.compose.tree.FileTree
import com.mibcxb.widget.compose.tree.SmbTreeItem
import com.mibcxb.widget.compose.tree.TreeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.File
import java.io.FileFilter

class BrowseViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager
) : AbsViewModel(cacheApi) {
    private val _treeRoots = mutableStateListOf<TreeItem>()
    val treeRoots: SnapshotStateList<TreeItem> get() = _treeRoots

    @Suppress("DEPRECATION")
    val fileTree: FileTree
        get() = FileTree(_treeRoots.filterIsInstance<FileItem>().toList())

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

    private val fileFilter: FileStubFilter = { stub ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            stub.isDirectory() -> true
            stub.isFile() -> extNames.contains(stub.extension)
            else -> false
        }
    }

    private val treeFileFilter = FileFilter { file ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            file.isHidden -> false
            file.isFile -> extNames.contains(file.extension)
            file.isDirectory -> true
            else -> false
        }
    }

    private val _selectedTreeItem = mutableStateOf<TreeItem?>(null)
    val selectedTreeItem: State<TreeItem?> get() = _selectedTreeItem

    private var treeInitializing = false

    fun initFileTree() {
        if (treeInitializing) return
        if (_treeRoots.isNotEmpty()) {
            syncSmbTree()
            return
        }
        treeInitializing = true
        viewModelScope.launch(Dispatchers.IO) {
            if (_treeRoots.isNotEmpty()) return@launch
            val localRoots = File.listRoots().filter { it.canRead() }.map { FileItem(it) }
            _treeRoots.addAll(localRoots)
            val firstLocal = localRoots.firstOrNull()
            if (firstLocal != null) {
                _selectedTreeItem.value = firstLocal
                _fileStub.value = FileStubImpl(firstLocal.file).apply { refreshList(fileFilter) }
                _filePath.value = firstLocal.path
            }
            syncSmbTree()
            treeInitializing = false
        }
    }

    fun syncSmbTree() {
        val smbSessions = smbManager.connectedSessions
        val currentKeys = smbSessions.map { "${it.host}:${it.share}" }.toSet()

        val existingSmbRoots = _treeRoots.filterIsInstance<SmbTreeItem>()
        val toRemove = existingSmbRoots.filter { "${it.host}:${it.share}" !in currentKeys }
        if (toRemove.isNotEmpty()) {
            _treeRoots.removeAll(toRemove)
            // Reset fileStub if current view is from a disconnected share
            val curStub = _fileStub.value
            if (curStub is SmbFileStub) {
                val smbKey = "${curStub.host}:${curStub.share}"
                if (smbKey !in currentKeys) {
                    resetToFirstLocalRoot()
                }
            }
        }

        val existingKeys = _treeRoots.filterIsInstance<SmbTreeItem>()
            .map { "${it.host}:${it.share}" }.toSet()

        for (session in smbSessions) {
            val key = "${session.host}:${session.share}"
            if (key !in existingKeys) {
                _treeRoots.add(SmbTreeItem(session.host, session.share, "", session, isRoot = true))
            }
        }
    }

    private fun resetToFirstLocalRoot() {
        val firstLocal = _treeRoots.filterIsInstance<FileItem>().firstOrNull() ?: return
        _selectedTreeItem.value = firstLocal
        _fileStub.value = FileStubImpl(firstLocal.file).apply { refreshList(fileFilter) }
        _filePath.value = firstLocal.path
    }

    fun singleClickTreeItem(item: FileItem) {
        if (_selectedTreeItem.value != item) {
            _selectedTreeItem.value = item
        }
        changeFileStub(item.file)
        changePreviewImageStub(item.file)
    }

    fun doubleClickTreeItem(item: FileItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList(treeFileFilter)
        }
    }

    fun singleClickSmbTreeItem(item: SmbTreeItem) {
        if (_selectedTreeItem.value != item) {
            _selectedTreeItem.value = item
        }
        val smbStub = SmbFileStub.create(item.host, item.share, item.remotePath, item.session)
        smbStub.refreshList(fileFilter)
        _fileStub.value = smbStub
        _filePath.value = SmbFileStub.buildSmbPath(item.host, item.share, item.remotePath)
    }

    fun doubleClickSmbTreeItem(item: SmbTreeItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList()
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
        if (newPath.isNotBlank()) {
            val smbPath = SmbFileStub.parseSmbPath(newPath)
            if (smbPath != null) {
                val (host, share, remotePath) = smbPath
                changeSmbFileStub(host, share, remotePath)
            } else {
                changeFileStub(File(newPath))
            }
        }
    }

    fun goToParentPath() {
        val curStub = _fileStub.value
        when (curStub) {
            is FileStubImpl -> {
                val newFile = curStub.file.parentFile
                if (newFile != null) {
                    changeFileStub(newFile)
                }
            }
            is SmbFileStub -> {
                val parentPath = curStub.remotePath.substringBeforeLast("/", "")
                if (parentPath != curStub.remotePath) {
                    changeSmbFileStub(curStub.host, curStub.share, parentPath)
                }
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

    private fun changeSmbFileStub(host: String, share: String, remotePath: String) {
        val session = smbManager.getSession(host, share) ?: return
        val stub = SmbFileStub.create(host, share, remotePath, session)
        stub.refreshList(fileFilter)
        _fileStub.value = stub
        _filePath.value = SmbFileStub.buildSmbPath(host, share, remotePath)
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
