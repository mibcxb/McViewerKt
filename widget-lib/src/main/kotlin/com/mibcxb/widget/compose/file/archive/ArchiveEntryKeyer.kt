package com.mibcxb.widget.compose.file.archive

import coil3.key.Keyer
import coil3.request.Options

class ArchiveEntryKeyer: Keyer<ArchiveEntryStub> {
    override fun key(data: ArchiveEntryStub, options: Options): String = "archiveentry:${data.path}"
}