package com.mibcxb.viewer.archive

import com.mibcxb.widget.compose.file.FileStub
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import java.io.InputStream

class ZipFileAccessor(fileStub: FileStub) : ArchiveAccessor(fileStub) {
    private var _zipFile: ZipFile? = null

    override fun prepare() {
        _zipFile = kotlin.runCatching {
            val builder = ZipFile.Builder()
            val zipFile = builder.setFile(fileStub.path).get()
            zipFile
        }.onFailure { logger.warn(logTag, it.message, it) }.getOrNull()
    }

    override fun release() {
        IOUtils.closeQuietly(_zipFile)
        _zipFile = null
    }

    override fun getEntryList(filter: (ArchiveEntryStub) -> Boolean): List<ArchiveEntryStub> {
        val zipFile = _zipFile ?: return emptyList()
        val subList = mutableListOf<ArchiveEntryStub>()
        val entries = zipFile.entries
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            val zipEntryStub = ZipEntryStub(zipEntry)
            if (filter(zipEntryStub)) {
                subList.add(zipEntryStub)
            }
        }
        return subList
    }

    override fun getInputStream(stub: ArchiveEntryStub): InputStream? {
        val zipFile = _zipFile
        if (zipFile != null) {
            val archiveEntry = stub.archiveEntry
            if (archiveEntry is ZipArchiveEntry) {
                return zipFile.getInputStream(archiveEntry)
            }
        }
        return null
    }
}