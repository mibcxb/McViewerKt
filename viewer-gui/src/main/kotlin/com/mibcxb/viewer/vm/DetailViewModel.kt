package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemFilter
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.LocalSource
import com.mibcxb.widget.compose.file.samba.SambaSource
import com.mibcxb.widget.compose.file.samba.SmbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetailViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager? = null
) : AbsViewModel(cacheApi) {

    private val _parentItem = mutableStateOf(emptyViewerItem)
    val parentItem: State<ViewerItem> get() = _parentItem

    private val _currentPath = mutableStateOf("")
    val currentPath: State<String> get() = _currentPath

    private val _showList = mutableStateOf(false)
    val showList: State<Boolean> get() = _showList

    private val fileFilter: ViewerItemFilter = { item ->
        val extNames = FileTypes.images.flatMap { it.extensions.toList() }
        when {
            item.isFile -> extNames.contains(item.extension)
            else -> false
        }
    }

    companion object {
        private val emptyViewerItem = ViewerItem(ViewerPath.parse(""))
    }

    fun initFilePath(filepath: String) {
        viewModelScope.launch {
            val vp = ViewerPath.parse(filepath)
            when (vp.scheme) {
                ViewerPath.Scheme.Samba -> initSambaFilePath(vp)
                ViewerPath.Scheme.File -> initLocalFilePath(vp)
            }
        }
    }

    private suspend fun initSambaFilePath(vp: ViewerPath) {
        val raw = vp.basePath.removePrefix("//")
        val parts = raw.split("/").filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val host = parts[0]
        val share = parts[1]
        val remotePath = parts.drop(2).joinToString("/")
        val session = smbManager?.getSession(host, share) ?: return
        withContext(Dispatchers.IO) {
            val source = SambaSource(session)
            val item = ViewerItem(ViewerPath.create(host, share, remotePath), source)
            if (!item.isFile) return@withContext
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentVp = ViewerPath.create(host, share, parentPath)
            _parentItem.value = ViewerItem(parentVp, source).apply { refreshList(fileFilter) }
            _currentPath.value = vp.raw
        }
    }

    private suspend fun initLocalFilePath(vp: ViewerPath) {
        withContext(Dispatchers.IO) {
            val file = File(vp.basePath)
            if (file.exists() && file.isFile) {
                val parent = file.parentFile
                if (parent != null) {
                    val source = LocalSource()
                    _parentItem.value = ViewerItem(ViewerPath.create(parent), source).apply { refreshList(fileFilter) }
                }
                _currentPath.value = vp.raw
            }
        }
    }

    fun next() { change(1) }
    fun prev() { change(-1) }

    private fun change(delta: Int) {
        val parent = _parentItem.value
        if (!parent.isDirectory || parent.children.isEmpty()) return
        val currentPath = _currentPath.value
        if (currentPath.isBlank()) return
        val subFiles = parent.children
        val curIndex = subFiles.indexOfFirst { it.path.raw == currentPath }
        if (curIndex == -1) return
        val newIndex = curIndex + delta
        if (newIndex !in subFiles.indices) return
        val target = subFiles[newIndex]
        changeFilePath(target.path.raw)
    }

    fun changeFilePath(newPath: String) {
        if (_currentPath.value != newPath) {
            _currentPath.value = newPath
        }
    }
}
