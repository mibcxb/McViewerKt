package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.viewer.archive.ArchiveAccessor
import com.mibcxb.viewer.archive.ArchiveAccessorFactory
import com.mibcxb.viewer.archive.ArchiveEntryStub
import com.mibcxb.viewer.archive.SevenZAccessor
import com.mibcxb.viewer.archive.ZipFileAccessor
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import okio.Buffer
import java.io.File

class ArchiveViewModel : AbsViewModel() {
    private val _filepath = mutableStateOf("")
    val filepath: State<String> get() = _filepath

    private val _subEntryList = mutableStateListOf<ArchiveEntryStub>()
    val subEntryList: SnapshotStateList<ArchiveEntryStub> get() = _subEntryList

    private val _subEntryStub = mutableStateOf<FileStub?>(null)
    val subEntryStub: State<FileStub?> get() = _subEntryStub

    private val accessorFactory: ArchiveAccessorFactory = ArchiveAccessorFactory { fileStub ->
        when (fileStub.fileType) {
            FileType.ZIP -> ZipFileAccessor(fileStub)
            FileType.SevenZ -> SevenZAccessor(fileStub)
            else -> null
        }
    }
    private var archiveAccessor: ArchiveAccessor? = null

    fun initFilePath(filepath: String) {
        if (_filepath.value != filepath) {
            _filepath.value = filepath
            prepareArchive()
        }
    }

    private fun prepareArchive() {
        val archivePath = _filepath.value
        val archiveFile = File(archivePath)
        if (!archiveFile.exists() || !archiveFile.isFile) {
            return
        }

        val accessor = accessorFactory.createArchiveAccessor(FileStubImpl(archiveFile))
        if (accessor != null) {
            archiveAccessor = accessor.apply { prepare() }
            val extensions = listOf(FileType.JPG, FileType.PNG).flatMap { it.extensions.toList() }.toTypedArray()
            val imgEntryList = accessor.getEntryList { stub ->
                !stub.isDirectory() && stub.extension in extensions
            }
            _subEntryList.clear()
            _subEntryList.addAll(imgEntryList)
        }
    }

    fun singleClickListItem(stub: FileStub) {
        if (_subEntryStub.value != stub) {
            _subEntryStub.value = stub
        }
    }

    fun getSubEntryData(stub: ArchiveEntryStub): Buffer? {
        val accessor = archiveAccessor ?: return null
        val stream = accessor.getInputStream(stub) ?: return null
        return stream.use { stream ->
            val output = Buffer()
            val buffer = ByteArray(8 * 1024)
            var length: Int
            while (stream.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            output
        }
    }

    fun getSubEntryMime(stub: ArchiveEntryStub): String? = when (stub.fileType) {
        FileType.JPG -> "image/jpeg"
        FileType.PNG -> "image/png"
        FileType.SVG -> "image/svg+xml"
        else -> null
    }

    override fun onCleared() {
        archiveAccessor?.release()
        archiveAccessor = null
    }
}