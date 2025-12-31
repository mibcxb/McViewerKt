package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.file.FileTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter

class DetailViewModel(cacheApi: CacheApi = CacheSqlite()) : AbsViewModel(cacheApi) {

    private val _parentStub = mutableStateOf<FileStub>(FileStubNone)
    val parentStub: State<FileStub> get() = _parentStub

    private val _filePath = mutableStateOf("")
    val filePath: State<String> get() = _filePath

    private val _showList = mutableStateOf(false)
    val showList: State<Boolean> get() = _showList

    private val fileFilter: FileFilter = FileFilter { file ->
        val extNames = FileTypes.images.flatMap { it.extensions.toList() }
        when {
            file.isHidden -> false
            file.isFile -> extNames.contains(file.extension)
            else -> false
        }
    }

    fun initFilePath(filepath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(filepath)
            if (file.exists() && file.isFile) {
                val parent = file.parentFile
                if (parent != null) {
                    _parentStub.value = FileStubImpl(parent).apply { refreshList(fileFilter) }
                }
                _filePath.value = filepath
            }
        }
    }

    fun next() {
        change(1)
    }

    fun prev() {
        change(-1)
    }

    private fun change(delta: Int) {
        val fileStub = parentStub.value
        if (!fileStub.isDirectory() || fileStub.subCount == 0) {
            return
        }
        val filePath = filePath.value
        if (filePath.isBlank()) {
            return
        }
        val subFiles = fileStub.subFiles
        val curIndex = subFiles.indexOfFirst { it.path == filePath }
        if (curIndex == -1) {
            return
        }
        val newIndex = curIndex + delta
        if (newIndex !in subFiles.indices) {
            return
        }
        val target = subFiles[newIndex]
        changeFilePath(target.path)
    }

    fun changeFilePath(newPath: String) {
        if (_filePath.value != newPath) {
            _filePath.value = newPath
        }
    }
}