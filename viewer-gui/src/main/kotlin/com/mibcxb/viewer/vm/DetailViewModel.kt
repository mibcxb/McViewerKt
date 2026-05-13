package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubFilter
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.samba.SmbFileStub
import com.mibcxb.widget.compose.file.samba.SmbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetailViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager? = null
) : AbsViewModel(cacheApi) {

    private val _parentStub = mutableStateOf<FileStub>(FileStubNone)
    val parentStub: State<FileStub> get() = _parentStub

    private val _filePath = mutableStateOf("")
    val filePath: State<String> get() = _filePath

    private val _showList = mutableStateOf(false)
    val showList: State<Boolean> get() = _showList

    private val fileFilter: FileStubFilter = { stub ->
        val extNames = FileTypes.images.flatMap { it.extensions.toList() }
        when {
            stub.isFile() -> extNames.contains(stub.extension)
            else -> false
        }
    }

    fun initFilePath(filepath: String) {
        viewModelScope.launch {
            val smbPath = SmbFileStub.parseSmbPath(filepath)
            if (smbPath != null) {
                initSmbFilePath(smbPath, filepath)
            } else {
                initLocalFilePath(filepath)
            }
        }
    }

    private suspend fun initSmbFilePath(smbPath: Triple<String, String, String>, filepath: String) {
        val (host, share, remotePath) = smbPath
        val session = smbManager?.getSession(host, share) ?: return
        withContext(Dispatchers.IO) {
            val stub = SmbFileStub.create(host, share, remotePath, session)
            if (!stub.exists() || !stub.isFile()) return@withContext
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentStub = SmbFileStub.create(host, share, parentPath, session)
            parentStub.refreshList(fileFilter)
            _parentStub.value = parentStub
            _filePath.value = filepath
        }
    }

    private suspend fun initLocalFilePath(filepath: String) {
        withContext(Dispatchers.IO) {
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