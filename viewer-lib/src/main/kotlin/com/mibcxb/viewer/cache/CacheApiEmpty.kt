package com.mibcxb.viewer.cache

object CacheApiEmpty : CacheApi {
    override fun isPathExists(path: String): Boolean = false
    override fun insertCacheThumb(path: String, data: ByteArray): Boolean = false
    override fun obtainCacheThumb(path: String): ByteArray? = null
    override fun insertSmbConnection(conn: SmbConnection): Boolean = false
    override fun deleteSmbConnection(host: String, share: String): Boolean = false
    override fun obtainAllSmbConnections(): List<SmbConnection> = emptyList()
}