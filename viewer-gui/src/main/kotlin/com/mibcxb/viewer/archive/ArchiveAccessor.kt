package com.mibcxb.viewer.archive

import com.mibcxb.viewer.log.LogApi
import com.mibcxb.widget.compose.file.FileStub
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

abstract class ArchiveAccessor(val fileStub: FileStub) : LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    abstract fun prepare()
    abstract fun release()

    abstract fun getEntryList(filter: (ArchiveEntryStub) -> Boolean = { true }): List<ArchiveEntryStub>

    abstract fun getInputStream(stub: ArchiveEntryStub): InputStream?
}