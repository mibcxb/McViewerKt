package com.mibcxb.widget.compose.file.archive

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

abstract class ArchiveAccessor(val filePath: String) {
    protected val logTag: String = javaClass.simpleName
    protected val logger: Logger = LoggerFactory.getLogger(logTag)

    abstract fun prepare()
    abstract fun release()

    abstract fun getEntryList(filter: (ArchiveEntryStub) -> Boolean = { true }): List<ArchiveEntryStub>

    abstract fun getInputStream(stub: ArchiveEntryStub): InputStream?
}
