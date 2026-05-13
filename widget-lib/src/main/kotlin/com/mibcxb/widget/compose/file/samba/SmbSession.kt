package com.mibcxb.widget.compose.file.samba

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.EnumSet

class SmbSession(
    val host: String,
    val share: String,
    private val client: SMBClient
) {
    private val logger = LoggerFactory.getLogger("SmbSession/$host/$share")

    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    val isConnected: Boolean
        get() = connection?.isConnected == true

    val key: String get() = "$host:$share"

    fun connect(auth: SmbAuth = SmbAuth()) {
        disconnect()

        val conn = client.connect(host)
        connection = conn

        val authCtx = AuthenticationContext(
            auth.username ?: "guest",
            auth.password?.toCharArray() ?: CharArray(0),
            auth.domain
        )
        val sess = conn.authenticate(authCtx)
        session = sess

        val share = sess.connectShare(share) as? DiskShare
            ?: throw IllegalStateException("Share $share is not a disk share")
        diskShare = share

        logger.info("Connected to //$host/$share")
    }

    fun disconnect() {
        runCatching { diskShare?.close() }
        diskShare = null

        runCatching { session?.close() }
        session = null

        runCatching { connection?.close() }
        connection = null

        logger.info("Disconnected from //$host/$share")
    }

    fun listFiles(remotePath: String): List<FileIdBothDirectoryInformation> {
        val share = diskShare ?: throw IllegalStateException("Not connected to //$host/$share")
        val path = if (remotePath.isNotEmpty() && remotePath != "/") remotePath else ""
        return try {
            share.list(path, "*")
        } catch (e: SMBApiException) {
            logger.warn("Failed to list $path: ${e.message}")
            emptyList()
        }
    }

    fun getFileInfo(remotePath: String): FileIdBothDirectoryInformation? {
        val share = diskShare ?: return null
        val parentPath = remotePath.substringBeforeLast("/", "")
        val fileName = remotePath.substringAfterLast("/")
        return try {
            share.list(parentPath, fileName).firstOrNull()
        } catch (e: SMBApiException) {
            logger.warn("Failed to stat $remotePath: ${e.message}")
            null
        }
    }

    fun getInputStream(remotePath: String): InputStream? {
        val share = diskShare ?: return null
        return try {
            val smbFile = share.openFile(
                remotePath,
                EnumSet.of(AccessMask.FILE_READ_DATA),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            smbFile.inputStream
        } catch (e: SMBApiException) {
            logger.warn("Failed to open $remotePath: ${e.message}")
            null
        }
    }

    fun exists(remotePath: String): Boolean {
        val share = diskShare ?: return false
        return try {
            share.fileExists(remotePath)
        } catch (e: SMBApiException) {
            false
        }
    }

    fun isDirectory(remotePath: String): Boolean {
        val share = diskShare ?: return false
        return try {
            share.folderExists(remotePath)
        } catch (e: SMBApiException) {
            false
        }
    }
}
