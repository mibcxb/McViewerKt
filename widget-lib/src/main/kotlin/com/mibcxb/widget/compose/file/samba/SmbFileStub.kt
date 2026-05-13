package com.mibcxb.widget.compose.file.samba

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.protocol.commons.EnumWithValue
import com.mibcxb.widget.compose.file.FileStub
import com.mibcxb.widget.compose.file.FileStubFilter
import com.mibcxb.widget.compose.file.FileType
import java.io.InputStream
class SmbFileStub internal constructor(
    val host: String,
    val share: String,
    val remotePath: String,
    private val fileInfo: FileIdBothDirectoryInformation?,
    private val session: SmbSession
) : FileStub {

    override val path: String get() = buildSmbPath(host, share, remotePath)

    override val name: String
        get() = remotePath.substringAfterLast("/").ifEmpty { share }

    override val extension: String
        get() = name.substringAfterLast(".", "").lowercase()

    override val fileType: FileType = if (fileInfo == null) {
        FileType.DIR
    } else if (EnumWithValue.EnumUtils.isSet(fileInfo.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }

    override val length: Long
        get() = fileInfo?.endOfFile ?: 0L

    override val lastModified: Long
        get() = fileInfo?.lastWriteTime?.toEpochMillis() ?: 0L

    override val createdAt: Long
        get() = fileInfo?.creationTime?.toEpochMillis() ?: lastModified

    override val subFiles: SnapshotStateList<FileStub> = mutableStateListOf()

    override fun refreshList(filter: FileStubFilter) {
        if (!isDirectory()) return
        synchronized(subFiles) {
            subFiles.clear()
            val entries = session.listFiles(remotePath)
            val childStubs = entries
                .filter { it.fileName != "." && it.fileName != ".." }
                .map { entry ->
                    val childPath = if (remotePath.isEmpty() || remotePath == "/") {
                        entry.fileName
                    } else {
                        "$remotePath/${entry.fileName}"
                    }
                    SmbFileStub(host, share, childPath, entry, session)
                }
                .filter { stub ->
                    when {
                        EnumWithValue.EnumUtils.isSet(
                            stub.fileInfo?.fileAttributes ?: 0,
                            FileAttributes.FILE_ATTRIBUTE_HIDDEN
                        ) -> false
                        else -> filter(stub)
                    }
                }
            subFiles.addAll(childStubs)
        }
    }

    override fun refreshStub(newStub: FileStub) {
        synchronized(subFiles) {
            val curStub = subFiles.find { it.path == newStub.path }
            if (curStub != null) {
                val index = subFiles.indexOf(curStub)
                subFiles[index] = newStub
            }
        }
    }

    override fun exists(): Boolean {
        if (fileInfo != null) return true
        if (remotePath.isEmpty()) return session.isConnected
        return session.exists(remotePath)
    }

    override fun isFile(): Boolean = fileInfo != null &&
        !EnumWithValue.EnumUtils.isSet(fileInfo.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)

    fun getInputStream(): InputStream? = session.getInputStream(remotePath)

    companion object {
        fun buildSmbPath(host: String, share: String, remotePath: String): String {
            val cleanPath = remotePath.trim('/')
            return if (cleanPath.isEmpty()) {
                "//$host/$share"
            } else {
                "//$host/$share/$cleanPath"
            }
        }

        fun parseSmbPath(path: String): Triple<String, String, String>? {
            val cleaned = path.removePrefix("smb:").removePrefix("//").removePrefix("\\\\")
            val parts = cleaned.split("/", "\\").filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            return Triple(parts[0], parts[1], parts.drop(2).joinToString("/"))
        }

        fun create(host: String, share: String, remotePath: String, session: SmbSession): SmbFileStub {
            return SmbFileStub(host, share, remotePath, null, session)
        }

        fun createRoot(host: String, share: String, session: SmbSession): SmbFileStub {
            return SmbFileStub(host, share, "", null, session)
        }
    }
}
