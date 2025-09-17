package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.common.http.MimeTypes
import com.mibcxb.widget.compose.file.archive.ArchiveAccessor
import com.mibcxb.widget.compose.file.archive.ArchiveAccessorFactory
import com.mibcxb.widget.compose.file.archive.ArchiveEntryStub
import com.mibcxb.widget.compose.file.archive.SevenZAccessor
import com.mibcxb.widget.compose.file.archive.ZipFileAccessor
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubImpl
import com.mibcxb.widget.compose.file.FileType
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

    fun getSubEntryMime(stub: ArchiveEntryStub): String? = MimeTypes.optMimeTypeByExtension(stub.extension)

    override fun onCleared() {
        archiveAccessor?.release()
        archiveAccessor = null
    }
}