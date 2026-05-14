package com.mibcxb.widget.compose.file.samba

import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.protocol.commons.EnumWithValue
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.ViewerSource
import java.io.InputStream

class SambaSource(private val session: SmbSession) : ViewerSource {
    val host: String get() = session.host
    val share: String get() = session.share

    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val remotePath = parseRemotePath(path)
        val entries = session.listFiles(remotePath)
        return entries
            .filter { it.fileName != "." && it.fileName != ".." }
            .filter {
                !EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_HIDDEN)
            }
            .map { entry ->
                val childPath = if (remotePath.isEmpty() || remotePath == "/") {
                    entry.fileName
                } else {
                    "$remotePath/${entry.fileName}"
                }
                ViewerPath.create(host, share, childPath)
            }
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val remotePath = parseRemotePath(path)
        return session.getInputStream(remotePath)
    }

    override fun exists(path: ViewerPath): Boolean {
        val remotePath = parseRemotePath(path)
        return if (remotePath.isEmpty()) session.isConnected
        else session.exists(remotePath)
    }

    private fun parseRemotePath(path: ViewerPath): String {
        val raw = path.basePath
        val prefix = "//$host/$share"
        return raw.removePrefix(prefix).trim('/')
    }
}
