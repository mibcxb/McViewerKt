package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileStubNone
import com.mibcxb.widget.compose.tree.FileItem
import com.mibcxb.widget.compose.tree.FileTree
import com.mibcxb.widget.compose.file.FileType
import java.io.File
import java.io.FileFilter

class HomeViewModel : AbsViewModel() {
    private val _fileTree = FileTree()
    val fileTree: FileTree get() = _fileTree

    private val _fileStub = mutableStateOf<FileStub>(FileStubNone)
    val fileStub: State<FileStub> get() = _fileStub

    private val _filePath = mutableStateOf("")
    val filePath: State<String> get() = _filePath

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
        if (item.fileType == FileType.DIR && _fileStub.value.path != item.path) {
            _fileStub.value = FileStubImpl(item.file).apply { refreshList(fileFilter) }
            _filePath.value = item.path
        }
    }

    fun doubleClickTreeItem(item: FileItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList(fileFilter)
        }
    }
}