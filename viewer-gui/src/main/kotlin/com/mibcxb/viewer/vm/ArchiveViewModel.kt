package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.common.http.MimeTypes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.archive.ArchiveAccessor
import com.mibcxb.widget.compose.file.archive.ArchiveAccessorFactory
import com.mibcxb.widget.compose.file.archive.ArchiveEntryStub
import com.mibcxb.widget.compose.file.archive.SevenZAccessor
import com.mibcxb.widget.compose.file.archive.ZipFileAccessor
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.FileType
import okio.Buffer
import java.io.File

class ArchiveViewModel(cacheApi: CacheApi = CacheSqlite()) : AbsViewModel(cacheApi) {
    private val _filepath = mutableStateOf("")
    val filepath: State<String> get() = _filepath

    private val _subEntryList = mutableStateListOf<ViewerItem>()
    val subEntryList: SnapshotStateList<ViewerItem> get() = _subEntryList

    private val _subEntryItem = mutableStateOf<ViewerItem?>(null)
    val subEntryItem: State<ViewerItem?> get() = _subEntryItem

    private val accessorFactory: ArchiveAccessorFactory = ArchiveAccessorFactory { fileType, filePath ->
        when (fileType) {
            FileType.ZIP -> ZipFileAccessor(filePath)
            FileType.SevenZ -> SevenZAccessor(filePath)
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
        val vp = ViewerPath.parse(archivePath)
        val archiveFile = File(vp.basePath)
        if (!archiveFile.exists() || !archiveFile.isFile) return

        val accessor = accessorFactory.createArchiveAccessor(vp.fileType, vp.basePath)
        if (accessor != null) {
            archiveAccessor = accessor.apply { prepare() }
            val extensions = listOf(FileType.JPG, FileType.PNG).flatMap { it.extensions.toList() }.toTypedArray()
            val imgEntryList = accessor.getEntryList { stub ->
                !stub.isDirectory() && stub.extension in extensions
            }
            _subEntryList.clear()
            _subEntryList.addAll(imgEntryList.map { entry ->
                ViewerItem(
                    ViewerPath.createArchived(vp, entry.archiveEntry.name),
                    null
                )
            })
        }
    }

    fun singleClickListItem(item: ViewerItem) {
        if (_subEntryItem.value != item) {
            _subEntryItem.value = item
        }
    }

    fun getSubEntryData(item: ViewerItem): Buffer? {
        val accessor = archiveAccessor ?: return null
        val stub = ArchiveEntryStub.getByPath(accessor, item.path.entryPath) ?: return null
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

    fun getSubEntryMime(item: ViewerItem): String? = MimeTypes.optMimeTypeByExtension(item.extension)

    override fun onCleared() {
        archiveAccessor?.release()
        archiveAccessor = null
    }
}
