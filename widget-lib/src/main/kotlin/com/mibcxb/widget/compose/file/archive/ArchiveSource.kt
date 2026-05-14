package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.ViewerSource
import java.io.InputStream

class ArchiveSource(
    private val accessor: ArchiveAccessor
) : ViewerSource {

    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val entries = accessor.getEntryList()
        return entries.map { entry ->
            ViewerPath.createArchived(path, entry.archiveEntry.name)
        }
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val stub = ArchiveEntryStub.getByPath(accessor, path.entryPath) ?: return null
        return accessor.getInputStream(stub)
    }

    override fun exists(path: ViewerPath): Boolean {
        return ArchiveEntryStub.getByPath(accessor, path.entryPath) != null
    }
}
