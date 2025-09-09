package com.mibcxb.viewer.cache

object CacheApiEmpty : CacheApi {
    override fun isPathExists(path: String): Boolean = false
    override fun insertCacheThumb(path: String, data: ByteArray): Boolean = false
    override fun obtainCacheThumb(path: String): ByteArray? = null
}