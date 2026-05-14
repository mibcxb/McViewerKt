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
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.LocalSource
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemFilter
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.samba.SambaSource
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileSortType
import com.mibcxb.widget.compose.tree.FileTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.File

class BrowseViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager
) : AbsViewModel(cacheApi) {
    private val _treeRoots = mutableStateListOf<ViewerItem>()
    val treeRoots: SnapshotStateList<ViewerItem> get() = _treeRoots

    val fileTree: FileTree
        get() = FileTree(_treeRoots.toList())

    private val _currentItem = mutableStateOf(emptyViewerItem)
    val currentItem: State<ViewerItem> get() = _currentItem

    private val _currentPath = mutableStateOf("")
    val currentPath: State<String> get() = _currentPath

    private val _previewImageItem = mutableStateOf(emptyViewerItem)
    val previewImageItem: State<ViewerItem> get() = _previewImageItem

    private val _fileTypeList = mutableStateListOf(*(FileTypes.images + FileTypes.archives))
    val fileTypeList: SnapshotStateList<FileType> get() = _fileTypeList

    private val _searchName = mutableStateOf("")
    val searchName: State<String> get() = _searchName

    private val _fileSortType = mutableStateOf(FileSortType.Filename)
    val fileSortType: State<FileSortType> get() = _fileSortType

    private val _fileGridSize = mutableStateOf(FileGridSize.Middle)
    val fileGridSize: State<FileGridSize> get() = _fileGridSize

    private val fileFilter: ViewerItemFilter = { item ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            item.isDirectory -> true
            item.isFile -> extNames.contains(item.extension)
            else -> false
        }
    }

    private val _selectedTreeItem = mutableStateOf<ViewerItem?>(null)
    val selectedTreeItem: State<ViewerItem?> get() = _selectedTreeItem

    private var treeInitializing = false

    companion object {
        private val emptyViewerItem = ViewerItem(ViewerPath.parse(""))
    }

    fun initFileTree() {
        if (treeInitializing) return
        if (_treeRoots.isNotEmpty()) {
            syncSmbTree()
            return
        }
        treeInitializing = true
        viewModelScope.launch(Dispatchers.IO) {
            if (_treeRoots.isNotEmpty()) return@launch
            val localSource = LocalSource()
            val localRoots = File.listRoots()
                .filter { it.canRead() }
                .map { ViewerItem(ViewerPath.create(it), localSource) }
            _treeRoots.addAll(localRoots)
            val firstLocal = localRoots.firstOrNull()
            if (firstLocal != null) {
                _selectedTreeItem.value = firstLocal
                _currentItem.value = firstLocal.apply { refreshList(fileFilter) }
                _currentPath.value = firstLocal.path.raw
            }
            syncSmbTree()
            treeInitializing = false
        }
    }

    fun syncSmbTree() {
        val smbSessions = smbManager.connectedSessions
        val currentKeys = smbSessions.map { "${it.host}:${it.share}" }.toSet()

        val existingSmbRoots = _treeRoots.filter { it.isSamba }
        val toRemove = existingSmbRoots.filter {
            val parsed = ViewerPath.parse(it.id)
            val raw = parsed.basePath.removePrefix("//")
            val parts = raw.split("/").filter { s -> s.isNotEmpty() }
            val key = if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
            key !in currentKeys
        }
        if (toRemove.isNotEmpty()) {
            _treeRoots.removeAll(toRemove)
            val curItem = _currentItem.value
            if (curItem.isSamba) {
                val parsed = ViewerPath.parse(curItem.id)
                val raw = parsed.basePath.removePrefix("//")
                val parts = raw.split("/").filter { s -> s.isNotEmpty() }
                val key = if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
                if (key !in currentKeys) {
                    resetToFirstLocalRoot()
                }
            }
        }

        val existingKeys = _treeRoots.filter { it.isSamba }
            .map {
                val parsed = ViewerPath.parse(it.id)
                val raw = parsed.basePath.removePrefix("//")
                val parts = raw.split("/").filter { s -> s.isNotEmpty() }
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
            }.toSet()

        for (session in smbSessions) {
            val key = "${session.host}:${session.share}"
            if (key !in existingKeys) {
                val source = SambaSource(session)
                val path = ViewerPath.create(session.host, session.share, "")
                val item = ViewerItem(path, source)
                _treeRoots.add(item)
            }
        }
    }

    private fun resetToFirstLocalRoot() {
        val firstLocal = _treeRoots.firstOrNull { it.isLocal } ?: return
        _selectedTreeItem.value = firstLocal
        _currentItem.value = firstLocal.apply { refreshList(fileFilter) }
        _currentPath.value = firstLocal.path.raw
    }

    fun singleClickTreeItem(item: ViewerItem) {
        if (_selectedTreeItem.value != item) {
            _selectedTreeItem.value = item
        }
        changeCurrentItem(item.path.basePath)
    }

    fun doubleClickTreeItem(item: ViewerItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList(fileFilter)
            changeCurrentItem(item.path.raw)
        }
    }

    fun singleClickGridItem(item: ViewerItem) {
        if (item.isLocal) {
            _previewImageItem.value = item
        }
    }

    fun doubleClickGridItem(item: ViewerItem) {
        if (item.isDirectory) {
            _currentItem.value = item.apply { refreshList(fileFilter) }
            _currentPath.value = item.path.raw
        }
    }

    fun changeFilePath(newPath: String) {
        if (_currentPath.value != newPath) {
            _currentPath.value = newPath
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
        goToTargetPath(_currentPath.value)
    }

    fun goToTargetPath(newPath: String) {
        if (newPath.isNotBlank()) {
            changeCurrentItem(newPath)
        }
    }

    fun goToParentPath() {
        val curItem = _currentItem.value
        val parentRaw = curItem.path.parentPath
        if (parentRaw != curItem.path.raw) {
            changeCurrentItem(parentRaw)
        }
    }

    fun refreshCurrent() {
        _currentItem.value.refreshList(fileFilter)
    }

    private fun changeCurrentItem(pathStr: String) {
        val vp = ViewerPath.parse(pathStr)
        when (vp.scheme) {
            ViewerPath.Scheme.File -> changeFileItem(vp)
            ViewerPath.Scheme.Samba -> changeSambaItem(vp)
        }
    }

    private fun changeFileItem(vp: ViewerPath) {
        val dir = File(vp.basePath)
        if (!dir.exists() || !dir.isDirectory) return
        if (_currentItem.value.path.raw == vp.raw) return
        val source = LocalSource()
        _currentItem.value = ViewerItem(vp, source).apply { refreshList(fileFilter) }
        _currentPath.value = vp.raw
    }

    private fun changeSambaItem(vp: ViewerPath) {
        val raw = vp.basePath.removePrefix("//")
        val parts = raw.split("/").filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val host = parts[0]
        val share = parts[1]
        val remotePath = parts.drop(2).joinToString("/")
        val session = smbManager.getSession(host, share) ?: return
        val source = SambaSource(session)
        val fullPath = ViewerPath.create(host, share, remotePath)
        _currentItem.value = ViewerItem(fullPath, source).apply { refreshList(fileFilter) }
        _currentPath.value = fullPath.raw
    }
}
