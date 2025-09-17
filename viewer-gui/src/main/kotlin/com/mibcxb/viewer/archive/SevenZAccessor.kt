package com.mibcxb.viewer.archive

import com.mibcxb.widget.compose.file.FileStub
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.io.IOUtils
import java.io.InputStream

class SevenZAccessor(fileStub: FileStub) : ArchiveAccessor(fileStub) {

    private var _sevenZFile: SevenZFile? = null

    override fun prepare() {
        _sevenZFile = kotlin.runCatching {
            SevenZFile.Builder().setFile(fileStub.path).get()
        }.onFailure { logger.warn(logTag, it.message, it) }.getOrNull()
    }

    override fun release() {
        IOUtils.closeQuietly(_sevenZFile)
        _sevenZFile = null
    }

    override fun getEntryList(filter: (ArchiveEntryStub) -> Boolean): List<ArchiveEntryStub> {
        val sevenZFile = _sevenZFile ?: return emptyList()
        val subList = mutableListOf<ArchiveEntryStub>()
        val iterator = sevenZFile.entries.iterator()
        while (iterator.hasNext()) {
            val sevenZEntry = iterator.next()
            val sevenZEntryStub = SevenZEntryStub(sevenZEntry)
            if (filter(sevenZEntryStub)) {
                subList.add(sevenZEntryStub)
            }
        }
        return subList
    }

    override fun getInputStream(stub: ArchiveEntryStub): InputStream? {
        val sevenZFile = _sevenZFile
        if (sevenZFile != null) {
            val archiveEntry = stub.archiveEntry
            if (archiveEntry is SevenZArchiveEntry) {
                return sevenZFile.getInputStream(archiveEntry)
            }
        }
        return null
    }
}